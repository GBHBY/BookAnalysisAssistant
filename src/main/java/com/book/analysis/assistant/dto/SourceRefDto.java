package com.book.analysis.assistant.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 知识库检索引用条目（供 NDJSON 推送给前端，与正文分离）。
 *
 * @author guoyb
 * @since 2026-03-28
 */
@Data
@Accessors(chain = true)
public class SourceRefDto {

    /**
     * 文档/来源展示名（如 metadata doc_name）
     */
    private String docName;

    /**
     * 片段摘要（截断后）
     */
    private String snippet;

    /**
     * 检索得分（若元数据中存在）
     */
    private Double score;
}
