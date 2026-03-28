package com.book.analysis.assistant.service;

import com.book.analysis.assistant.dto.SourceRefDto;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 按单次流式对话回合（turnId）收集工具检索到的文档引用，供响应结束前统一下发。
 *
 * @author guoyb
 * @since 2026-03-28
 */
@Service
public class TurnCitationCollector {

    /**
     * 与 {@link org.springframework.ai.model.tool.ToolCallingChatOptions#setToolContext} 中 key 一致
     */
    public static final String TOOL_CONTEXT_TURN_ID_KEY = "streamTurnId";

    private static final int SNIPPET_MAX = 480;

    private final Map<String, CopyOnWriteArrayList<SourceRefDto>> citationsByTurn = new ConcurrentHashMap<>();

    public void initTurn(String turnId) {
        if (!StringUtils.hasText(turnId)) {
            return;
        }
        citationsByTurn.putIfAbsent(turnId, new CopyOnWriteArrayList<>());
    }

    public void appendDocuments(String turnId, List<Document> documents) {
        if (!StringUtils.hasText(turnId) || documents == null || documents.isEmpty()) {
            return;
        }
        CopyOnWriteArrayList<SourceRefDto> list =
                citationsByTurn.computeIfAbsent(turnId, k -> new CopyOnWriteArrayList<>());
        for (Document doc : documents) {
            list.add(toRef(doc));
        }
    }

    /**
     * 取出并移除该回合引用，避免泄漏。
     */
    public List<SourceRefDto> drainAndRemove(String turnId) {
        if (!StringUtils.hasText(turnId)) {
            return List.of();
        }
        CopyOnWriteArrayList<SourceRefDto> removed = citationsByTurn.remove(turnId);
        if (removed == null || removed.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(removed));
    }

    public void clearTurn(String turnId) {
        if (StringUtils.hasText(turnId)) {
            citationsByTurn.remove(turnId);
        }
    }

    private static SourceRefDto toRef(Document doc) {
        SourceRefDto dto = new SourceRefDto();
        Map<String, Object> meta = doc.getMetadata();
        String docName = "";
        if (meta != null) {
            Object name = meta.get("doc_name");
            if (name != null) {
                docName = name.toString();
            }
            Object score = meta.get("distance");
            if (score == null) {
                score = meta.get("score");
            }
            if (score instanceof Number n) {
                dto.setScore(n.doubleValue());
            }
        }
        dto.setDocName(docName);
        String text = doc.getText() != null ? doc.getText() : "";
        if (text.length() > SNIPPET_MAX) {
            text = text.substring(0, SNIPPET_MAX) + "…";
        }
        dto.setSnippet(text);
        return dto;
    }
}
