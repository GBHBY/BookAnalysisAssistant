package com.book.analysis.assistant.emdedding;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文档元数据信息
 *
 * @author guoyb
 * @since 2026-03-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMetadata {
    /**
     * 书名
     */
    @JSONField(name = "domain")
    private String domain;

    /**
     * 内容分类（如小说类型）
     */
    @JSONField(name = "category")
    private String category;

    /**
     * 关键词列表（3-5个）
     */
    @JSONField(name = "keywords")
    private List<String> keywords;

    /**
     * 文档一句话总结（不超过10个字）
     */
    @JSONField(name = "summary")
    private String summary;
}
