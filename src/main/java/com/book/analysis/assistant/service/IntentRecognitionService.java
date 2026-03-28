package com.book.analysis.assistant.service;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.book.analysis.assistant.dto.IntentResult;
import com.book.analysis.assistant.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IntentRecognitionService {
    
    @Autowired
    private DashScopeChatModel chatModel;
    
    public IntentResult recognizeIntent(String userQuery) {
        String systemPrompt = buildIntentRecognitionPrompt();
        
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));
        messages.add(new UserMessage(userQuery));
        
        try {
            String response = chatModel.call(new Prompt(messages)).getResult().getOutput().getText();
            log.info("意图识别结果: {}", response);
            
            return parseIntentResponse(response, userQuery);
        } catch (Exception e) {
            log.error("意图识别失败", e);
            return new IntentResult(IntentType.UNKNOWN, userQuery, new HashMap<>(), 0.0);
        }
    }
    
    private String buildIntentRecognitionPrompt() {
        return "你是一个智能客服助手的意图识别模块。请分析用户的输入，识别用户的意图并提取关键参数。\n\n" +
                "支持的意图类型：\n" +
                "1. QUERY_ORDER - 查询订单信息（关键词：查询、订单号、订单状态等）\n" +
                "2. CREATE_ORDER - 创建新订单（关键词：下单、购买、邮寄、寄送等）\n" +
                "3. KNOWLEDGE_QUERY - 知识库查询（关键词：运输规则、产品信息、公司政策、能否邮寄、禁运、限制等）\n" +
                "4. COMMON_SENSE - 常识问题（时间、日期、天气、简单计算、常识性问题等）\n" +
                "5. GENERAL_CHAT - 通用对话（问候、闲聊、感谢等）\n\n" +
                "请以JSON格式返回结果：\n" +
                "{\n" +
                "  \"intentType\": \"意图类型\",\n" +
                "  \"confidence\": 0.95,\n" +
                "  \"extractedParams\": {\n" +
                "    \"key\": \"value\"\n" +
                "  }\n" +
                "}\n\n" +
                "参数提取规则：\n" +
                "- QUERY_ORDER: 提取orderNo（订单号）\n" +
                "- CREATE_ORDER: 提取customerName, phone, address, productName, quantity等\n" +
                "- KNOWLEDGE_QUERY: 提取query（查询关键词）\n" +
                "- COMMON_SENSE: 无需提取参数\n" +
                "- GENERAL_CHAT: 无需提取参数\n\n" +
                "只返回JSON，不要有其他文字。";
    }
    
    private IntentResult parseIntentResponse(String response, String originalQuery) {
        try {
            // 清理可能的markdown代码块标记
            response = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
            
            JSONObject json = JSON.parseObject(response);
            IntentType intentType = IntentType.valueOf(json.getString("intentType"));
            Double confidence = json.getDouble("confidence");
            Map<String, Object> params = json.getJSONObject("extractedParams");
            
            return new IntentResult(intentType, originalQuery, params, confidence);
        } catch (Exception e) {
            log.error("解析意图结果失败: {}", response, e);
            return new IntentResult(IntentType.UNKNOWN, originalQuery, new HashMap<>(), 0.0);
        }
    }
}
