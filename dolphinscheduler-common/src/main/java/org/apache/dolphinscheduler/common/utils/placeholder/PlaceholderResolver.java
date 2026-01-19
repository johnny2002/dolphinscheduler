package org.apache.dolphinscheduler.common.utils.placeholder;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量替换工具类 - 基础版本
 * 将字符串中的 ${variable} 替换为实际值
 */
public class PlaceholderResolver {

    // 默认变量正则表达式
    private static final Pattern DEFAULT_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 替换字符串中的变量（使用 Map）
     */
    public static String replaceVariables(String input, Map<String, Object> variables) {
        if (input == null || input.isEmpty() || !input.contains("${")) {
            return input;
        }

        if (variables == null || variables.isEmpty()) {
            return input;
        }
        Matcher matcher = DEFAULT_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1).trim();
            Object value = variables.get(variableName);

            if (value != null) {
                // 替换变量
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            } else {
                // 变量未找到，保持原样
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    /**
     * 替换字符串中的变量（使用可变参数）
     */
    public static String replaceVariables(String input, String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("参数必须是键值对形式");
        }

        Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            variables.put(keyValues[i], keyValues[i + 1]);
        }

        return replaceVariables(input, variables);
    }

//    /**
//     * 测试示例
//     */
//    public static void main(String[] args) {
//        // 示例1：使用 Map
//        String template = "Hello, ${name}! Today is ${day}. The temperature is ${temperature}°C and weather is ${weather}.";
//
//        Map<String, Object> variables = new HashMap<>();
//        variables.put("name", "Alice");
//        variables.put("day", "Monday");
//        variables.put("temperature", 25);
//        String result = replaceVariables(template, variables);
//        System.out.println("示例1结果: " + result);
//        // 输出: Hello, Alice! Today is Monday. The temperature is 25°C.
//        // 示例2：使用可变参数
//        String template2 = "User: ${username}, Email: ${email}, Age: ${age}";
//        String result2 = replaceVariables(template2,
//                "username", "john_doe",
//                "email", "john@example.com",
//                "age", "30");
//        System.out.println("示例2结果: " + result2);
//        // 输出: User: john_doe, Email: john@example.com, Age: 30
//
//        // 示例3：嵌套变量（不支持）
//        String template3 = "Base URL: ${base_url}/api/${version}/users";
//        Map<String, Object> vars3 = new HashMap<>();
//        vars3.put("base_url", "https://api.example.com");
//        vars3.put("version", "v1");
//        System.out.println("示例3结果: " + replaceVariables(template3, vars3));
//        // 输出: Base URL: https://api.example.com/api/v1/users
//
//        // 示例4：未定义的变量
//        String template4 = "Missing: ${undefined_var}, Defined: ${defined_var}";
//        Map<String, Object> vars4 = new HashMap<>();
//        vars4.put("defined_var", "value");
//        System.out.println("示例4结果: " + replaceVariables(template4, vars4));
//        System.out.println("示例5结果: " + replaceVariables("nothing to be replaced", vars4));
//        // 输出: Missing: ${undefined_var}, Defined: value
//    }
}