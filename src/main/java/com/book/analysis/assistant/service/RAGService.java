package com.book.analysis.assistant.service;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentCloudReader;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentTransformer;
import com.book.analysis.assistant.entity.KnowledgeDocument;
import com.book.analysis.assistant.mapper.KnowledgeDocumentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class RAGService {
    
    @Autowired
    private DashScopeApi dashScopeApi;
    
    @Autowired
    private DashScopeChatModel chatModel;
    
    @Autowired
    private KnowledgeDocumentMapper documentMapper;
    
    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;
    
    /**
     * 上传文档并向量化
     */
    public String uploadDocument(MultipartFile file, String category) throws Exception {
        // 1. 保存文件
        String documentId = UUID.randomUUID().toString();
        String fileName = file.getOriginalFilename();
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        Path filePath = uploadPath.resolve(documentId + "_" + fileName);
        file.transferTo(filePath.toFile());
        
        log.info("文件已保存: {}", filePath);
        
        // 2. 解析文档并切分
        DashScopeDocumentCloudReader reader = new DashScopeDocumentCloudReader(
            filePath.toString(), dashScopeApi, null);
        List<Document> documents = reader.get();
        
        // 3. 文档转换
        DashScopeDocumentTransformer transformer = new DashScopeDocumentTransformer(dashScopeApi);
        List<Document> transformedDocs = transformer.apply(documents);
        
        log.info("文档切分完成，共{}个chunk", transformedDocs.size());
        
        // 4. 向量化并存储
        // 注意：实际使用时需要配置DashScope的向量存储服务
        // 这里需要调用DashScope的API将文档存储到向量数据库
        
        // 5. 保存文档元数据到数据库
        KnowledgeDocument knowledgeDoc = new KnowledgeDocument();
        knowledgeDoc.setDocumentId(documentId);
        knowledgeDoc.setFileName(fileName);
        knowledgeDoc.setFileType(getFileExtension(fileName));
        knowledgeDoc.setCategory(category);
        knowledgeDoc.setVectorStoreId("vector_store");
        knowledgeDoc.setChunkCount(transformedDocs.size());
        knowledgeDoc.setStatus("completed");
        knowledgeDoc.setCreateTime(LocalDateTime.now());
        knowledgeDoc.setUpdateTime(LocalDateTime.now());
        
        documentMapper.insert(knowledgeDoc);
        
        log.info("文档上传完成: {}", documentId);
        return documentId;
    }
    
    /**
     * 检索相关文档
     * 注意：此方法需要根据实际的DashScope API进行实现
     */
    public List<Document> retrieveDocuments(String query, int topK) {
        try {
            // TODO: 实现文档检索逻辑
            // 需要配置DashScope的向量存储服务
            log.warn("文档检索功能待实现，需要配置DashScope向量存储");
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("检索文档失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * RAG问答 - 简化版本，直接使用大模型
     */
    public String ragQuery(String query, String category) {
        try {
            // 简化实现：直接使用大模型回答
            // 实际使用时需要先检索相关文档，然后结合文档内容生成答案
            
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的客服助手，请根据用户的问题提供帮助。"));
            messages.add(new UserMessage(query));
            
            String response = chatModel.call(new Prompt(messages))
                .getResult()
                .getOutput()
                .getText();
            
            return response;
        } catch (Exception e) {
            log.error("RAG查询失败", e);
            return "抱歉，查询知识库时出现错误";
        }
    }
    
    /**
     * 删除文档
     */
    public void deleteDocument(String documentId) {
        KnowledgeDocument doc = documentMapper.selectById(documentId);
        if (doc != null) {
            documentMapper.deleteById(documentId);
            
            // 删除文件
            try {
                Path filePath = Paths.get(uploadDir, documentId + "_" + doc.getFileName());
                Files.deleteIfExists(filePath);
            } catch (Exception e) {
                log.error("删除文件失败", e);
            }
        }
    }
    
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}
