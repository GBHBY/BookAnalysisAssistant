package com.book.analysis.assistant.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.book.analysis.assistant.entity.ConversationHistory;
import com.book.analysis.assistant.mapper.ConversationHistoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService extends ServiceImpl<ConversationHistoryMapper, ConversationHistory> {
    
    public void saveMessage(String sessionId, String role, String content) {
        ConversationHistory history = new ConversationHistory();
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        this.save(history);
    }
    
    public List<ConversationHistory> getConversationHistory(String sessionId) {
        QueryWrapper<ConversationHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("session_id", sessionId)
                .orderByDesc("create_time")
                .last("limit 30");
        List<ConversationHistory> list = this.list(wrapper);
        return list;
    }
}
