package com.book.analysis.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * 会话历史压缩摘要，供后续轮次节省 token、保持指代一致。
 *
 * @author guoyb
 * @since 2026/3/28
 */
@Service
@Slf4j
public class MemeryService {

    private static final int MAX_RAW_CHARS = 8000;

    private static final PromptTemplate TEMPLATE = new PromptTemplate("""
            你是武侠小说知识助手背后的「会话摘要」模块。请阅读下列多轮对话，输出一段给大模型用的简短上下文（中文）。

            要求：
            1. 用条目式（- 开头），总长度控制在 400 字以内。
            2. 保留：已讨论的作品/人物/门派/武功/情节要点、用户的偏好或明确结论、尚未回答清楚的追问。
            3. 丢弃：寒暄、重复表述、与武侠知识无关的闲聊。
            4. 不要编造对话里不存在的情节；信息不足就写「（无更多有效历史）」。
            5. 不要输出 JSON、不要分角色复述全文，只输出摘要正文。

            对话原文：
            {{history}}
            """);

    @Autowired
    private ZhiPuAiChatModel zhiPuAiChatModel;

    /**
     * 将历史对话压缩为摘要；无历史或调用失败时返回空串，由调用方决定是否回退原文。
     *
     * @param historyContext 已拼接好的历史文本
     * @return 摘要文本，可能为空
     */
    public String summaryHistoricalDialogues(String historyContext) {
        if (!StringUtils.hasText(historyContext)) {
            return "";
        }
        String clipped = historyContext.length() > MAX_RAW_CHARS
                ? historyContext.substring(0, MAX_RAW_CHARS) + "\n…（已截断）"
                : historyContext;
        try {
            Prompt prompt = TEMPLATE.create(Map.of("history", clipped));
            String text = zhiPuAiChatModel.call(prompt).getResult().getOutput().getText();
            if (!StringUtils.hasText(text)) {
                return "";
            }
            return text.strip();
        } catch (Exception e) {
            log.warn("历史会话摘要失败，将使用空摘要", e);
            return "";
        }
    }
}
