package com.book.analysis.assistant.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工具调用配额管理器（按单次对话请求维度）
 *
 * @author guoyb
 * @since 2026-03-27
 */
@Service
public class ToolCallQuotaService {

    private static final ThreadLocal<String> CURRENT_TURN_ID = new ThreadLocal<>();

    private final Map<String, AtomicInteger> turnCallCountMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> turnLimitMap = new ConcurrentHashMap<>();

    public void initTurn(String turnId, int maxCalls) {
        turnCallCountMap.put(turnId, new AtomicInteger(0));
        turnLimitMap.put(turnId, maxCalls);
    }

    public void bindTurn(String turnId) {
        CURRENT_TURN_ID.set(turnId);
    }

    public void clearBoundTurn() {
        CURRENT_TURN_ID.remove();
    }

    /**
     * 当前请求线程绑定的回合 id；工具可能在同线程执行时可与 {@link TurnCitationCollector} 兜底关联。
     */
    public String getCurrentTurnId() {
        return CURRENT_TURN_ID.get();
    }

    public void clearTurn(String turnId) {
        turnCallCountMap.remove(turnId);
        turnLimitMap.remove(turnId);
    }

    public AcquireResult tryAcquireForCurrentTurn() {
        String turnId = CURRENT_TURN_ID.get();
        if (turnId == null) {
            return new AcquireResult(true, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        AtomicInteger counter = turnCallCountMap.get(turnId);
        Integer limit = turnLimitMap.get(turnId);
        if (counter == null || limit == null) {
            return new AcquireResult(true, Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        int current = counter.incrementAndGet();
        if (current > limit) {
            return new AcquireResult(false, current, limit);
        }
        return new AcquireResult(true, current, limit);
    }

    public record AcquireResult(boolean allowed, int currentCalls, int maxCalls) {
    }
}
