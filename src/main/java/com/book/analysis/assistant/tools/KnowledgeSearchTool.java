package com.book.analysis.assistant.tools;

import com.book.analysis.assistant.emdedding.EmbeddingService;
import com.book.analysis.assistant.service.ToolCallQuotaService;
import com.book.analysis.assistant.service.TurnCitationCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 武侠小说知识库搜索工具
 * 用于搜索武侠小说相关内容，回答关于武侠小说情节、人物、武功、门派等问题
 * 
 * @author guoyb
 * @since 2026-03-21
 */
@Component
@Slf4j
public class KnowledgeSearchTool {
    
    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private ToolCallQuotaService toolCallQuotaService;

    @Autowired
    private TurnCitationCollector turnCitationCollector;

    @Tool(
        name = "searchKnowledgeBase", 
        description = "搜索武侠小说知识库。适用于查询：武侠小说情节、人物介绍、武功招式、门派信息、故事背景等。" +
                     "不适用于：订单查询、闲聊、常识问题（如时间日期天气等）。"
    )
    public String searchKnowledgeBase(
            @ToolParam(description = "搜索关键词或问题，例如：张无忌的武功、峨眉派的历史") String query,
            @ToolParam(description = "返回结果数量，默认5，最大10") Integer limit,
            ToolContext toolContext) {
        ToolCallQuotaService.AcquireResult acquireResult = toolCallQuotaService.tryAcquireForCurrentTurn();
        if (!acquireResult.allowed()) {
            log.warn("知识库搜索调用超限 - 当前: {}, 上限: {}", acquireResult.currentCalls(), acquireResult.maxCalls());
            return "知识库搜索调用次数已达到上限，请基于已有信息直接回答。";
        }

        if (limit == null || limit <= 0) {
            limit = 5;
        }
        if (limit > 10) {
            limit = 10;
        }

        log.info("执行知识库搜索 - 查询: {}, 限制: {}", query, limit);
        
        try {
            List<Document> documents = embeddingService.searchSimilarWithFilter(query, limit, null);

            if (documents == null || documents.isEmpty()) {
                return "未找到相关的知识库文档。";
            }

            String turnId = resolveTurnId(toolContext);
            turnCitationCollector.appendDocuments(turnId, documents);

            // 格式化返回结果
            StringBuilder result = new StringBuilder();
            result.append("找到 ").append(documents.size()).append(" 条相关武侠小说内容：\n\n");
            
            for (int i = 0; i < documents.size(); i++) {
                Document doc = documents.get(i);
                result.append("【片段").append(i + 1).append("】\n");
                result.append(doc.getText()).append("\n");
                
                // 添加元数据信息（如果有）
                if (!doc.getMetadata().isEmpty()) {
                    String docName = (String) doc.getMetadata().getOrDefault("doc_name", "");
                    if (!docName.isEmpty()) {
                        result.append("来源: ").append(docName).append("\n");
                    }
                }
                result.append("\n");
            }
            
            log.info("知识库搜索完成，返回 {} 条结果", documents.size());
            return result.toString();
            
        } catch (Exception e) {
            log.error("知识库搜索失败", e);
            return "知识库搜索出错: " + e.getMessage();
        }
    }

    private String resolveTurnId(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext() != null) {
            Object id = toolContext.getContext().get(TurnCitationCollector.TOOL_CONTEXT_TURN_ID_KEY);
            if (id != null && StringUtils.hasText(id.toString())) {
                return id.toString();
            }
        }
        String bound = toolCallQuotaService.getCurrentTurnId();
        return bound != null ? bound : "";
    }
}
