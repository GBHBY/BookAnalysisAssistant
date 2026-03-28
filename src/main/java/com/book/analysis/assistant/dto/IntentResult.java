package com.book.analysis.assistant.dto;

import com.book.analysis.assistant.enums.IntentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntentResult {
    private IntentType intentType;
    private String originalQuery;
    private Map<String, Object> extractedParams;
    private Double confidence;
}
