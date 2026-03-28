package com.book.analysis.assistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.book.analysis.assistant.entity.ConversationHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationHistoryMapper extends BaseMapper<ConversationHistory> {
}
