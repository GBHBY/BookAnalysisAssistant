package com.book.analysis.assistant.emdedding;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 过滤表达式构建器
 * 将用户输入的过滤条件转换为 pgvector 可识别的表达式格式
 * 
 * @author guoyb
 * @since 2026-03-29
 */
public class FilterExpressionBuilder {

    /**
     * 从 Map 构建过滤表达式
     * 
     * 输入格式（JSON Map）：
     * - {"domain": "人民的名义"}
     * - {"category": "人物"}
     * - {"domain": "人民的名义", "category": "人物"}
     * 
     * 输出格式（Spring AI 过滤表达式格式，pgvector 将自动转换为 jsonpath）：
     * - "domain == '人民的名义'"
     * - "category == '人物'"
     * - "domain == '人民的名义' && category == '人物'"
     * 
     * @return Spring AI 过滤表达式格式
     */
    public static String buildFilter(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        return map.entrySet().stream()
                .map(e -> e.getKey() + " == \"" + e.getValue() + "\"")
                .collect(Collectors.joining(" AND "));
    }

    /**
     * 构建过滤表达式（静态方法，支持字符串格式）
     * 
     * 输入格式支持：
     * - "domain=人民的名义"
     * - "category=言情小说"
     * - "domain=人民的名义 AND category=剧情"
     * - "" 或 null（空过滤条件）
     * 
     * 输出格式（pgvector 标准）：
     * - "metadata.domain == '人民的名义'"
     * - "metadata.category == '言情小说'"
     * - "metadata.domain == '人民的名义' && metadata.category == '剧情'"
     * - "" （空字符串）
     * 
     * @param filterInput 用户输入的过滤条件字符串
     * @return pgvector 标准的过滤表达式
     */
    public static String build(String filterInput) {
        if (StringUtils.isBlank(filterInput)) {
            return "";
        }

        String result = filterInput.trim();
        
        // 1. 替换 AND 为 &&（支持大小写）
        result = result.replaceAll("(?i)\\s+AND\\s+", " && ");
        
        // 2. 处理 key=value 格式，转换为 metadata.key == 'value'
        // 匹配模式：field_name=value，value 可能包含中文、数字等
        result = result.replaceAll(
            "([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*([^&]+?)(?=\\s*(?:&&|$))", 
            "metadata.$1 == '$2'"
        );
        
        // 3. 清理多余的空格（在 && 两边）
        result = result.replaceAll("\\s+&&\\s+", " && ");
        
        // 4. 清理单个值末尾的空格
        result = result.replaceAll("'\\s+", "'");
        result = result.replaceAll("\\s+'", "'");
        
        // 5. 最终 trim
        result = result.trim();
        
        return result;
    }

    /**
     * 验证过滤表达式是否有效
     * 简单的验证：检查是否有配对的单引号
     * 
     * @param filterExpression pgvector 格式的过滤表达式
     * @return 是否有效
     */
    public static boolean isValid(String filterExpression) {
        if (StringUtils.isBlank(filterExpression)) {
            return true;  // 空的过滤条件也是有效的
        }
        
        // 检查单引号是否配对
        long singleQuoteCount = filterExpression.chars().filter(ch -> ch == '\'').count();
        return singleQuoteCount % 2 == 0;
    }
}
