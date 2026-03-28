package com.book.analysis.assistant.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author guoyb
 * @since 2026/3/27
 */
@Service
@Slf4j
public class QueryService {

    private static final String REWRITE_PROMPT = """
            请结合下列对话历史，把用户「当前问题」改写为适合武侠小说知识库向量检索的一句话（仅输出改写结果，不要解释）。

            【对话历史】
            %s

            要求：
            1. 若存在指代（如他、她、那本、上次说的），根据历史补全为明确的人物、门派、情节或书名。
            2. 突出可检索关键词：人名、武功、门派、兵器、章节情节等。
            3. 避免模糊词与过长从句，一条检索 query 风格。
            """;

    @Autowired
    private ZhiPuAiChatModel chatModel;

    public String reWriteQuery(String query, String historyContext) {
        String history = historyContext == null ? "（无）" : historyContext;
        String systemPrompt = String.format(REWRITE_PROMPT, history);

        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(systemPrompt),
                        new UserMessage(query)
                )
        );
        String reWriteResult = chatModel.call(prompt).getResult().getOutput().getText();
        log.info("问题改写：{}", reWriteResult);
        return reWriteResult;


    }
}
