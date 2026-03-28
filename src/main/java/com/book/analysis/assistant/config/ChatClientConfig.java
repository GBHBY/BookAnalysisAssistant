package com.book.analysis.assistant.config;

import com.book.analysis.assistant.tools.KnowledgeSearchTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient 配置
 * 配置默认的工具集，让 AI 可以自动调用
 *
 * @author guoyb
 * @since 2026-03-21
 */
@Configuration
public class ChatClientConfig {

    /**
     * 配置 ChatClient，注册所有可用的工具
     * AI 会根据用户问题自动决定调用哪些工具
     */
    @Bean
    public ChatClient chatClient(ZhiPuAiChatModel chatModel, KnowledgeSearchTool knowledgeSearchTool) {

        return ChatClient.builder(chatModel)
                .defaultTools(
                        knowledgeSearchTool
                )
                .build();
    }
}
