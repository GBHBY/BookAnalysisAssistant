package com.book.analysis.assistant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文本导入请求对象
 *
 * @author GB
 * @since 2026-03-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TextImportRequest {

    /**
     * 文本内容
     */
    private String text;
}
