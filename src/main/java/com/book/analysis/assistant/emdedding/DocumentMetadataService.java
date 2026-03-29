package com.book.analysis.assistant.emdedding;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 文档元数据提取服务
 * 通过 AI 分析文档内容，提取 domain、category、keywords、summary 等信息
 *
 * @author guoyb
 * @since 2026-03-29
 */
@Service
@Slf4j
public class DocumentMetadataService {

    @Lazy
    @Autowired
    private ChatClient chatClient;

    /**
     * 从文档内容中提取元数据
     *
     * @param documentText 文档文本内容（通常是前几百个字符）
     * @return 提取的元数据信息
     */
    public DocumentMetadata extractMetadata(String documentText) {
        try {
            // 截取前 500 个字符用于分析（避免请求过长）
            String textToAnalyze = documentText.length() > 500 
                ? documentText.substring(0, 500) 
                : documentText;

            String prompt = buildPrompt(textToAnalyze);
            
            log.info("开始调用 AI 提取文档元数据...");
            
            String response = chatClient.prompt(prompt)
                    .call()
                    .content();

            log.info("AI 响应: {}", response);

            // 解析 AI 返回的 JSON
            DocumentMetadata metadata = parseResponse(response);
            log.info("元数据提取完成: {}", metadata);
            
            return metadata;
        } catch (Exception e) {
            log.error("提取文档元数据失败", e);
            // 返回默认元数据
            return DocumentMetadata.builder()
                    .domain("未知")
                    .category("其他")
                    .keywords(java.util.Collections.emptyList())
                    .summary("暂无总结")
                    .build();
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String documentText) {
        return String.format("""
            请从以下文本中提取文档信息，并以JSON格式返回。
            
            要求：
            1. domain（书名）：如"神雕侠侣"、"人民的名义"、"追问"等
            2. category（内容类型）：对于小说类，如"言情小说"、"科幻小说"、"悬疑小说"等
            3. keywords（关键词）：提取3-5个最能代表文档内容的关键词，返回为数组
            4. summary（总结）：用不超过10个字的中文总结该文档内容
            
            文本内容：
            %s
            
            请返回以下JSON格式的数据（仅返回JSON，不需要其他说明，请返回纯json字符串）：
            {
              "domain": "xxxxx",
              "category": "xxxxx",
              "keywords": ["关键词1", "关键词2", "关键词3"],
              "summary": "xxxxx"
            }
            """, documentText);
    }

    /**
     * 解析 AI 返回的响应
     */
    private DocumentMetadata parseResponse(String response) {
        try {
            // 先尝试提取 Markdown 代码块中的 JSON（如果有的话）
            String jsonContent = extractJsonFromMarkdown(response);
            
            // 尝试直接解析 JSON
            DocumentMetadata metadata = JSON.parseObject(jsonContent, DocumentMetadata.class);
            
            // 验证字段
            if (metadata.getDomain() == null || metadata.getDomain().isBlank()) {
                metadata.setDomain("未知");
            }
            if (metadata.getCategory() == null || metadata.getCategory().isBlank()) {
                metadata.setCategory("其他");
            }
            if (metadata.getKeywords() == null || metadata.getKeywords().isEmpty()) {
                metadata.setKeywords(java.util.Collections.emptyList());
            }
            if (metadata.getSummary() == null || metadata.getSummary().isBlank()) {
                metadata.setSummary("暂无总结");
            }
            
            return metadata;
        } catch (Exception e) {
            log.warn("解析 AI 响应失败，使用默认元数据", e);
            return DocumentMetadata.builder()
                    .domain("未知")
                    .category("其他")
                    .keywords(java.util.Collections.emptyList())
                    .summary("暂无总结")
                    .build();
        }
    }

    /**
     * 从 Markdown 代码块中提取 JSON
     * 处理类似以下格式的响应：
     * ```json
     * { ... }
     * ```
     */
    private String extractJsonFromMarkdown(String response) {
        // 如果响应包含 Markdown 代码块标记
        if (response.contains("```")) {
            // 匹配 ```...``` 中间的内容
            int startIdx = response.indexOf("```");
            int endIdx = response.lastIndexOf("```");
            
            if (startIdx != -1 && endIdx != -1 && startIdx < endIdx) {
                // 获取代码块内容
                String codeBlock = response.substring(startIdx + 3, endIdx).trim();
                
                // 如果代码块以 json 开头，移除它
                if (codeBlock.startsWith("json")) {
                    codeBlock = codeBlock.substring(4).trim();
                }
                
                log.debug("从 Markdown 代码块中提取的 JSON: {}", codeBlock);
                return codeBlock;
            }
        }
        
        // 如果没有 Markdown 代码块标记，直接返回原内容
        return response;
    }
}
