package com.book.analysis.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("conversation_history")
public class ConversationHistory {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}
