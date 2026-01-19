package org.apache.dolphinscheduler.plugin.task.datax;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;


import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 扩展的DataX参数类，支持参数替换
 */
public class EnhancedDataxParameters extends DataxParameters {

    // 原始JSON配置
    private String rawJson;

    // 替换后的JSON配置
    private String replacedJson;

    // 参数映射
    private Map<String, String> paramMapping;

    /**
     * 替换参数
     */
    public String replaceParameters(Map<String, Property> allParams) {
        if (StringUtils.isBlank(rawJson)) {
            return rawJson;
        }

        // 深度替换JSON中的所有参数
        try {
            replacedJson = deepReplaceJsonParameters(rawJson, allParams);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return replacedJson;
    }

    /**
     * 深度替换JSON参数
     */
    private String deepReplaceJsonParameters(String json, Map<String, Property> allParams) throws JsonProcessingException {
        // 解析JSON为Map
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);
        // 递归替换所有字符串值
        replaceInObject(jsonMap, allParams);

        // 转换回JSON字符串
        return objectMapper.writeValueAsString(jsonMap);
    }

    @SuppressWarnings("unchecked")
    private void replaceInObject(Object obj, Map<String, Property> allParams) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof String) {
                    // 替换字符串中的参数
                    String strValue = (String) value;
                    if (strValue.contains("${")) {
                        String replacedValue = replaceStringParameters(strValue, allParams);
                        entry.setValue(replacedValue);
                    }
                } else if (value instanceof List) {
                    // 处理列表
                    replaceInList((List<Object>) value, allParams);
                } else if (value instanceof Map) {
                    // 递归处理Map
                    replaceInObject(value, allParams);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void replaceInList(List<Object> list, Map<String, Property> allParams) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);

            if (item instanceof String) {
                String strItem = (String) item;
                if (strItem.contains("${")) {
                    String replacedItem = replaceStringParameters(strItem, allParams);
                    list.set(i, replacedItem);
                }
            } else if (item instanceof Map) {
                replaceInObject(item, allParams);
            } else if (item instanceof List) {
                replaceInList((List<Object>) item, allParams);
            }
        }
    }

    /**
     * 替换字符串中的参数
     */
    private String replaceStringParameters(String input, Map<String, Property> allParams) {
        String result = input;

        // 替换 ${} 格式
        result = replacePattern(result, "\\$\\{([^}]+)\\}", allParams);

        // 替换 $[] 格式（兼容旧格式）
        result = replacePattern(result, "\\$\\[([^]]+)\\]", allParams);

        return result;
    }

    private String replacePattern(String input, String patternStr, Map<String, Property> allParams) {
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String paramKey = matcher.group(1).trim();
            Property property = allParams.get(paramKey);

            if (property != null && property.getValue() != null) {
                String value = property.getValue();
                // 递归替换嵌套参数
                value = replaceStringParameters(value, allParams);
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            } else {
                // 保持原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    // getters and setters
    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public String getReplacedJson() {
        return replacedJson;
    }

    public void setReplacedJson(String replacedJson) {
        this.replacedJson = replacedJson;
    }
}