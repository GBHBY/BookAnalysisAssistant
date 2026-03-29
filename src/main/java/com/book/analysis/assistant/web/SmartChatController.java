package com.book.analysis.assistant.web;

import com.book.analysis.assistant.common.ApiResponse;
import com.book.analysis.assistant.dto.AgentResponse;
import com.book.analysis.assistant.dto.SourceRefDto;
import com.book.analysis.assistant.enums.AnswerType;
import com.book.analysis.assistant.service.ConversationService;
import com.book.analysis.assistant.service.MemeryService;
import com.book.analysis.assistant.service.QueryService;
import com.book.analysis.assistant.service.ToolCallQuotaService;
import com.book.analysis.assistant.service.TurnCitationCollector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 智能对话控制器
 * 使用 ChatClient + Function Calling 让 AI 自主决定调用哪些工具
 *
 * @author guoyb
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/api/smart-chat")
@Slf4j
@CrossOrigin
public class SmartChatController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private ToolCallQuotaService toolCallQuotaService;

    @Autowired
    private MemeryService memeryService;

    @Autowired
    private TurnCitationCollector turnCitationCollector;

    private static final int MAX_TOOL_CALLS_PER_TURN = 2;

    private static final String SYSTEM_TEMPLATE = """
            你是一个小说知识助手。
            
            你可以使用工具：
            - searchKnowledgeBase：用于查询小说内容（人物、情节等）
            
            【工具使用规则】
            1. 只有在问题涉及小说时才调用工具
            2. 优先使用“推荐查询”作为搜索关键词
            3. 最多调用工具 2 次
            4. 如果信息足够，请直接回答
            
            【回答要求】
            1. 回答要生动
            2. 如果使用了工具，请基于结果回答
            3. 不要编造内容
            
            请使用标准 Markdown 格式输出，要求：
            1. 标题前必须有换行
            2. 分隔线 --- 前后必须有空行
            3. 不要输出不完整的 Markdown 结构
            4. 保证每个段落独立
            
            """;


    /**
     * 智能对话接口（流式）
     * AI 自动决定是否调用工具，流式返回结果
     */
    @GetMapping(value = "/stream-chat", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<AgentResponse> streamChat(
            @RequestParam(name = "query") String query,
            @RequestParam(name = "sessionId", defaultValue = "default") String sessionId) {

        log.info("流式智能对话请求 - sessionId: {}, query: {}", sessionId, query);
        AtomicReference<String> logStr = new AtomicReference<>("");
        return Flux.create(sink -> {
            String turnId = UUID.randomUUID().toString();
            try {
                toolCallQuotaService.initTurn(turnId, MAX_TOOL_CALLS_PER_TURN);
                turnCitationCollector.initTurn(turnId);
                toolCallQuotaService.bindTurn(turnId);

                // 获取历史对话
                StringBuilder fullAnswer = new StringBuilder();
                String historyContext = buildHistoryContext(sessionId);
                log.info("完成历史对话加载");

                Mono<String> summaryMono = Mono.fromCallable(() -> memeryService.summaryHistoricalDialogues(historyContext))
                        .subscribeOn(Schedulers.boundedElastic())
                        .defaultIfEmpty("")
                        .onErrorResume(e -> {
                            log.warn("异步历史摘要失败", e);
                            return Mono.just("");
                        })
                        .cache();
                summaryMono.subscribe();

                Mono<String> rewriteMono = Mono.fromCallable(() -> queryService.reWriteQuery(query, historyContext))
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorResume(e -> {
                            log.error("异步问题重写失败", e);
                            return Mono.just(query);
                        })
                        .cache();

                rewriteMono
                        .doOnNext(rw -> log.info("完成问题重写"))
                        .doOnNext(rw -> sink.next(AgentResponse.of(rw, AnswerType.THINKING)))
                        .doOnNext(rw -> sink.next(AgentResponse.of("寻找更多有效的历史对话", AnswerType.THINKING)))
                        .flatMap(rw -> summaryMono.map(sum -> Tuples.of(rw, sum)))
                        .subscribe(tuple -> {
                            String reWriteQuery = tuple.getT1();
                            String summary = tuple.getT2();
                            log.info("历史摘要就绪（长度 {}）", summary.length());
                            String summaryForDisplay = StringUtils.hasText(summary)
                                    ? summary
                                    : historyContext;
                            if (StringUtils.hasText(summaryForDisplay)) {
                                sink.next(AgentResponse.of(summaryForDisplay, AnswerType.THINKING));
                            }
                            String contextForPrompt = StringUtils.hasText(summary) ? summary : historyContext;
                            Prompt prompt = buildPrompt(contextForPrompt, reWriteQuery, query);

                            chatClient.prompt(prompt)
                                    .options(ToolCallingChatOptions.builder()
                                            .toolContext(Map.of(TurnCitationCollector.TOOL_CONTEXT_TURN_ID_KEY, turnId))
                                            .build())
                                    .stream()
                                    .content()
                                    .doOnNext(content -> {
                                        if (content != null && !content.isEmpty()) {
                                            fullAnswer.append(content);
                                            sink.next(AgentResponse.of(content, AnswerType.FINALLY_ANSWER));
                                            logStr.set(logStr + content.replace("\n", "\\n"));

                                        }
                                    })
                                    .doOnComplete(() -> {
                                        List<SourceRefDto> refs = turnCitationCollector.drainAndRemove(turnId);
                                        if (!refs.isEmpty()) {
                                            sink.next(AgentResponse.builder()
                                                    .answer("")
                                                    .answerType(AnswerType.REFERENCES)
                                                    .references(refs)
                                                    .build());
                                            log.info("本回合下发知识库引用 {} 条", refs.size());
                                        }
                                        sink.next(AgentResponse.of("", AnswerType.LINE_BREAK));
                                        conversationService.saveMessage(sessionId, "user", query);
                                        conversationService.saveMessage(sessionId, "assistant", fullAnswer.toString());
                                        sink.complete();
                                        log.info(String.valueOf(logStr));
                                        toolCallQuotaService.clearTurn(turnId);
                                        toolCallQuotaService.clearBoundTurn();
                                    })
                                    .doOnError(error -> {
                                        log.error("流式回答生成失败", error);
                                        turnCitationCollector.clearTurn(turnId);
                                        toolCallQuotaService.clearTurn(turnId);
                                        toolCallQuotaService.clearBoundTurn();
                                        sink.error(error);
                                    })
                                    .subscribe();
                        }, err -> {
                            log.error("流式对话预处理失败", err);
                            turnCitationCollector.clearTurn(turnId);
                            toolCallQuotaService.clearTurn(turnId);
                            toolCallQuotaService.clearBoundTurn();
                            sink.error(err);
                        });

            } catch (Exception e) {
                log.error("流式对话处理失败", e);
                turnCitationCollector.clearTurn(turnId);
                toolCallQuotaService.clearTurn(turnId);
                toolCallQuotaService.clearBoundTurn();
                sink.next(AgentResponse.of("", AnswerType.FAIL));
                sink.error(e);
            }
        });
    }

    private Prompt buildPrompt(String summarizedOrRawHistory, String reWriteQuery, String query) {
        List<Message> messages = new ArrayList<>();
        if (StringUtils.hasText(summarizedOrRawHistory)) {
            messages.add(new SystemMessage("""
                    【会话摘要】
                    %s
                    """.formatted(summarizedOrRawHistory)));
        }
        messages.add(new UserMessage("当前问题：" + query));
        messages.add(new SystemMessage("""
                【推荐查询】
                %s
                """.formatted(reWriteQuery)));
        messages.add(new SystemMessage(SYSTEM_TEMPLATE));
        return new Prompt(messages);
    }


    /**
     * 构建历史对话上下文
     */
    private String buildHistoryContext(String sessionId) {
        StringBuilder context = new StringBuilder();

        conversationService.getConversationHistory(sessionId).stream()
                .forEach(h -> {
                    if ("user".equals(h.getRole())) {
                        context.append("用户: ").append(h.getContent()).append("\n");
                    } else if ("assistant".equals(h.getRole())) {
                        context.append("助手: ").append(h.getContent()).append("\n");
                    }
                });

        return context.toString();
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public ApiResponse<?> getHistory(@RequestParam(defaultValue = "default") String sessionId) {
        try {
            return ApiResponse.success(conversationService.getConversationHistory(sessionId));
        } catch (Exception e) {
            log.error("获取对话历史失败", e);
            return ApiResponse.error("获取历史失败: " + e.getMessage());
        }
    }


}
