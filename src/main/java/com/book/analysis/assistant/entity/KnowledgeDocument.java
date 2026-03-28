package com.book.analysis.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String documentId;
    private String fileName;
    private String fileType;
    private String category;
    private String content;
    private String vectorStoreId;
    private Integer chunkCount;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
