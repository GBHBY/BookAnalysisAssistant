package com.book.analysis.assistant.emdedding;

import cn.hutool.core.collection.ListUtil;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.transformer.splitter.SentenceSplitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本切分、Embedding 计算并写入 PG Vector.
 *
 * @author guoyb
 * @since 2026-03-11
 */
@Service
@Slf4j
public class EmbeddingService {
    @Autowired
    private DashScopeEmbeddingModel dashScopeEmbeddingModel;

    @Autowired
    private ZhiPuAiEmbeddingModel zhiPuAiEmbeddingModel;

    @Autowired
    private PgVectorWriteService pgVectorWriteService;

    @Qualifier("vectorStore")
    @Autowired
    private VectorStore vectorStore;

    @Qualifier("vectorStoreSplit")
    @Autowired
    private VectorStore vectorStoreSplit;

    @Lazy
    @Autowired
    private DocumentMetadataService documentMetadataService;


    /**
     * 向量相似性搜索
     *
     * @param query 查询文本
     * @param topK  返回最相似的前 K 个结果
     * @return 相似文档列表，包含原文和元数据
     */
    public List<Document> searchSimilar(String query, int topK) {
        log.info("执行向量搜索，查询: {}, topK: {}", query, topK);

        // 使用 VectorStore 的相似性搜索
        List<Document> results = vectorStore.similaritySearch(query);

        log.info("搜索完成，找到 {} 个相似文档", results.size());

        // 打印结果（可选）
        results.forEach(doc -> {
            log.debug("相似文档: {}, 元数据: {}",
                    doc.getText().substring(0, Math.min(100, doc.getText().length())),
                    doc.getMetadata());
        });

        return results;
    }

    /**
     * 向量相似性搜索（带相似度阈值）
     *
     * @param query     查询文本
     * @param topK      返回最相似的前 K 个结果
     * @param threshold 相似度阈值（0-1之间，越接近1越相似）
     * @return 相似文档列表
     */
    public List<Document> searchSimilarWithThreshold(String query, int topK, double threshold) {
        log.info("执行向量搜索（带阈值），查询: {}, topK: {}, threshold: {}", query, topK, threshold);

        // 使用 SearchRequest 进行更精细的控制
        SearchRequest searchRequest = SearchRequest.builder()
                .topK(topK)
                .similarityThreshold(threshold)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);

        log.info("搜索完成，找到 {} 个相似文档（阈值: {}）", results.size(), threshold);

        return results;
    }

    /**
     * 向量相似性搜索（带过滤条件）
     *
     * @param query            查询文本
     * @param topK             返回最相似的前 K 个结果
     * @param filterExpression 过滤条件 Map，支持字段：domain、category、keywords等
     * @return 相似文档列表
     */
    public List<Document> searchSimilarWithFilter(String query, int topK, Map<String, Object> filterExpression) {
        log.info("执行向量搜索（带过滤），查询: {}, topK: {}, filter: {}", query, topK, filterExpression);

        // 转换 Map 为 pgvector 支持的过滤表达式
        String pgvectorFilter = FilterExpressionBuilder.buildFilter(filterExpression);
        log.info("转换后的过滤表达式: {}", pgvectorFilter);

        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK);
        
        // 只在过滤表达式不为空时添加
        if (StringUtils.isNotBlank(pgvectorFilter)) {
            builder.filterExpression(pgvectorFilter);
        }

        List<Document> results = vectorStoreSplit.similaritySearch(builder.build());
        List<String> list = results.stream().map(Document::getText).toList();

        log.info("结果：{}", list);
        log.info("搜索完成，找到 {} 个相似文档", results.size());

        return results;
    }


    /**
     * Spring AI Embedding 转为 float[]（兼容 getOutput() 为 List&lt;Double&gt; 或 float[]）
     */
    private static float[] embeddingToFloatArray(Embedding embedding) {
        return embedding.getOutput();
    }

    public void embeddingFile(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        List<Document> documentList = readPdfFile(resource);
        log.info("源文件大小:{}：文本：{}", documentList.size(), documentList);
        List<Document> cleanResult = cleanFile(documentList);
        log.info("清洗后，大小:{}, 文本:{}", cleanResult.size(), cleanResult);
        
        // 提取文档元数据（仅从第一个文档提取）
        DocumentMetadata metadata = null;
        if (!cleanResult.isEmpty()) {
            String firstPageText = cleanResult.get(0).getText();
            metadata = documentMetadataService.extractMetadata(firstPageText);
            log.info("提取的文档元数据: {}", metadata);
        }
        
        List<Document> intelligentSplitFile = intelligentSplitFile(cleanResult);
        log.info("读取完成,开始进行插入到向量库");
        for (List<Document> documents : ListUtil.partition(intelligentSplitFile, 5)) {
            // 为每个文档块添加元数据
            List<Document> docsWithMetadata = enrichDocumentsWithMetadata(documents, metadata);
            vectorStoreSplit.add(docsWithMetadata);
        }
        // 将文档添加到向量存储
        log.info("插入向量库完成");
    }

    private List<Document> cleanFile(List<Document> documentList) {
        List<Document> result = new ArrayList<>();
        for (Document document : documentList) {
            String text = document.getText();
            text = text.replaceAll("[ \\t]{2,}", " ");
            text = text.replaceAll("\\n+", "\n");
            Document resultDocument = new Document(document.getId(), text, document.getMetadata());
            result.add(resultDocument);
        }
        return result;
    }

    /**
     * 为文档块添加元数据信息
     * 
     * @param documents 文档块列表
     * @param metadata 提取的元数据
     * @return 添加了元数据的文档列表
     */
    private List<Document> enrichDocumentsWithMetadata(List<Document> documents, DocumentMetadata metadata) {
        if (metadata == null) {
            return documents;
        }

        List<Document> result = new ArrayList<>(256);
        for (Document doc : documents) {
            Map<String, Object> enrichedMetadata = new HashMap<>(doc.getMetadata());
            
            // 添加提取的元数据信息
            enrichedMetadata.put("domain", metadata.getDomain());
            enrichedMetadata.put("category", metadata.getCategory());
            enrichedMetadata.put("keywords", metadata.getKeywords());
            enrichedMetadata.put("summary", metadata.getSummary());
            
            Document enrichedDoc = new Document(doc.getId(), doc.getText(), enrichedMetadata);
            result.add(enrichedDoc);
        }
        return result;
    }

    /**
     * 固定长度切分
     *
     * @param documentList
     * @return
     */
    private List<Document> FixedLengthSplitFile(List<Document> documentList) {

        // 一个文本块最多 600 tokens，如果一个文本就60个，那么chunk size 就是 60 ，那么就会受到minChunkSizeChars的影响
        // minChunkSizeChars 最小字符数，防止垃圾块，如果这个设置的过大，会导致短段落被丢弃
        // minChunkLengthToEmbed 最小发生 embedding 块大小
        // maxNumChunks 最多切多少块，太大会导致向量库过大，chunk 越多噪音越大
        // 是否保留分隔符

        TokenTextSplitter tokenTextSplitter =
                new TokenTextSplitter(500, 300, 5, 1000, true);
        List<Document> split = tokenTextSplitter.split(documentList);
        log.info("切分后:,大小:{}, 文本:{}", split.size(), split);
        return split;
    }

    private List<Document> intelligentSplitFile(List<Document> documentList) {
        List<Document> result = new ArrayList<>();
        SentenceSplitter sentenceSplitter = new SentenceSplitter(200);
        List<Document> split = sentenceSplitter.split(documentList);
        log.info("切分后:,大小:{}, 文本:{}", split.size(), split);
        for (Document document : split) {
            if(StringUtils.isNotBlank(document.getText())){
                result.add(document);
            }
        }
        return result;
    }

    private List<Document> readPdfFile(Resource resource) {
        // 构建 PDF 文档读取配置
        PdfDocumentReaderConfig pdfConfig = PdfDocumentReaderConfig.builder()
                // 配置页面文本提取格式
                // 页面下边距（单位：磅）
                // 用于忽略页脚信息，设置为 0 表示不忽略
                .withPageBottomMargin(0)
                // 页面上边距（单位：磅）
                // 用于忽略页眉信息，设置为 0 表示不忽略
                .withPageTopMargin(0)
                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                        // 要删除的底部文本行数
                        // 用于删除页面底部的指定行数的文本（如页码、脚注等）
                        // 设置为 0 表示不删除任何行
                        .withNumberOfBottomTextLinesToDelete(0)
                        // 是否左对齐文本
                        // 设置为 true 表示提取的文本进行左对齐处理
                        // 设置为 false 表示保持原始对齐方式
                        // 用于规范化文本格式
                        .withLeftAlignment(false)
                        // 要删除的顶部文本行数
                        // 用于删除页面顶部的指定行数的文本（如页眉、标题等）
                        // 设置为 0 表示不删除任何行
                        .withNumberOfTopTextLinesToDelete(0)
                        // 在删除文本前要跳过的顶部页数
                        // 用于指定从第几页开始删除顶部文本
                        // 设置为 0 表示从第一页开始删除
                        // 设置为 1 表示从第二页开始删除
                        // 用于保留首页的页眉等信息
                        .withNumberOfTopPagesToSkipBeforeDelete(0)
                        .build()
                )


                // 是否反转段落位置
                // 设置为 true 表示反转段落的顺序（从下到上）
                // 设置为 false 表示保持原始顺序（从上到下）
                // 用于处理某些特殊格式的 PDF 文档
                .withReversedParagraphPosition(false)
                // 每个 Document 对象包含的页数
                // 设置为 1 表示每页作为一个独立的 Document
                // 设置为 2 表示每 2 页作为一个 Document
                // 用于控制文档分割的粒度
                .withPagesPerDocument(1)
                .build();
        PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(resource, pdfConfig);
        log.info("开始读取");
        // 读取 PDF 文件，返回文档列表
        return pagePdfDocumentReader.read();
    }
}
