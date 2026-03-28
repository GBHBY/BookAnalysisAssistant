package com.book.analysis.assistant.enums;

/**
 * @author guoyb
 * @since 2026/3/28
 */
public enum AnswerType {
    /** 思考过程 **/
    THINKING,

    /** 最终回答 **/
    FINALLY_ANSWER,

    /** 对话引用 **/
    DOCUMENT_QUOTE,

    /** 本回合知识库检索引用列表（与 {@code AgentResponse#references} 配合） **/
    REFERENCES,

    /** 换行 **/
    LINE_BREAK,

    FAIL,

    ;

}
