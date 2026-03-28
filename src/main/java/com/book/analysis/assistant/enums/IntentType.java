package com.book.analysis.assistant.enums;

public enum IntentType {
    QUERY_ORDER("查询订单"),
    CREATE_ORDER("创建订单"),
    SHIPPING_RULES("运输规则查询"),
    KNOWLEDGE_QUERY("知识库查询"),
    COMMON_SENSE("常识问题"),
    GENERAL_CHAT("通用对话"),
    UNKNOWN("未知意图");
    
    private final String description;
    
    IntentType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
