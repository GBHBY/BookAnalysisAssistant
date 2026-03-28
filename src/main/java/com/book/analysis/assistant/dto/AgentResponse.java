package com.book.analysis.assistant.dto;

import com.book.analysis.assistant.enums.AnswerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AgentResponse {
    private String answer;
    private AnswerType answerType;

    /**
     * 知识库引用；一般仅在 {@link AnswerType#REFERENCES} 时非空。
     */
    private List<SourceRefDto> references;

    public static AgentResponse of(String answer, AnswerType answerType) {
        return AgentResponse.builder().answer(answer).answerType(answerType).build();
    }
}
