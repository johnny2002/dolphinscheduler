package org.apache.dolphinscheduler.server.master.runner.task.dynamic;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数值区间展开工具。
 *
 * <p>支持写法：
 * <ul>
 *   <li>普通值：{@code 1,2,3} 或 {@code a,b,c}</li>
 *   <li>整数区间：{@code [1..10]}、{@code [10..1]}（反向）、{@code [-3..3]}</li>
 *   <li>字母区间：{@code [a..z]}、{@code [Z..A]}（大小写需一致）</li>
 *   <li>混用：{@code [1..3],7,9}</li>
 * </ul>
 *
 * <p>非区间 token 原样保留；区间展开含两端；超上限/类型不匹配/大小写不一致抛
 * {@link IllegalArgumentException}。
 */
public final class ParameterRangeExpander {

    /** 单个整数区间的最大展开数量，防止 [1..999999] 打爆内存 */
    public static final int MAX_RANGE_SIZE = 10_000;

    /** 匹配 [start..end]，start/end 可为整数、单个小写字母、单个大写字母 */
    private static final Pattern RANGE_PATTERN =
            Pattern.compile("^\\[\\s*(-?\\d+|[a-zA-Z])\\s*\\.\\.\\s*(-?\\d+|[a-zA-Z])\\s*\\]$");

    /** 匹配 [ ... ] 整体（用于切分前保护区间 token，避免 separator 把区间切碎） */
    private static final Pattern BRACKET_BLOCK = Pattern.compile("\\[[^\\[\\]]*]");

    private ParameterRangeExpander() {
    }

    /**
     * 展开整串：按 separator 切分后逐个展开。
     * <p>切分时会先保护 {@code [...]} 区间块，避免 {@code separator="."} 把 {@code [1..3]} 切碎。
     *
     * @param value     原始值，如 {@code "1,2,[5..7]"}
     * @param separator 分隔符，如 {@code ","}
     * @return 展开后的值列表；value 为空返回空列表
     */
    public static List<String> expandAll(String value, String separator) {
        List<String> result = new ArrayList<>();
        if (StringUtils.isBlank(value)) {
            return result;
        }
        for (String token : safeSplit(value, separator)) {
            result.addAll(expandToken(token.trim()));
        }
        return result;
    }

    /**
     * 展开单个 token：匹配区间则展开，否则原样返回单元素列表；空串返回空列表。
     */
    public static List<String> expandToken(String token) {
        if (StringUtils.isBlank(token)) {
            return new ArrayList<>();
        }
        Matcher m = RANGE_PATTERN.matcher(token.trim());
        if (!m.matches()) {
            return new ArrayList<>(Arrays.asList(token));
        }

        String startStr = m.group(1);
        String endStr = m.group(2);

        // 1) 整数区间
        if (isInteger(startStr) && isInteger(endStr)) {
            int start = Integer.parseInt(startStr);
            int end = Integer.parseInt(endStr);
            int size = Math.abs(end - start) + 1;
            if (size > MAX_RANGE_SIZE) {
                throw new IllegalArgumentException(
                        "区间过大: " + token + "，展开数量=" + size + "，上限=" + MAX_RANGE_SIZE);
            }
            List<String> result = new ArrayList<>(size);
            int step = start <= end ? 1 : -1;
            for (int i = start; ; i += step) {
                result.add(String.valueOf(i));
                if (i == end) {
                    break;
                }
            }
            return result;
        }

        // 2) 单字母区间（大小写必须一致）
        if (isSingleLetter(startStr) && isSingleLetter(endStr)) {
            char s = startStr.charAt(0);
            char e = endStr.charAt(0);
            if (Character.isUpperCase(s) != Character.isUpperCase(e)) {
                throw new IllegalArgumentException("区间两端大小写不一致: " + token);
            }
            List<String> result = new ArrayList<>(Math.abs(e - s) + 1);
            int step = s <= e ? 1 : -1;
            for (char c = s; ; c += step) {
                result.add(String.valueOf(c));
                if (c == e) {
                    break;
                }
            }
            return result;
        }

        throw new IllegalArgumentException("区间两端类型不匹配（一端数字一端字母）: " + token);
    }

    // ==================== 内部工具 ====================

    /**
     * 先保护 {@code [...]} 区间块，再按 separator 切分，最后还原。
     * <p>占位符使用可见字符串而非 \u0000，避免被 trim() 剥掉边界字符。
     */
    private static List<String> safeSplit(String value, String separator) {
        if (StringUtils.isEmpty(separator)) {
            return new ArrayList<>(Arrays.asList(value));
        }

        final String placeholderPrefix = "__RANGE_PH_";
        final String placeholderSuffix = "__";

        List<String> blocks = new ArrayList<>();
        Matcher blockMatcher = BRACKET_BLOCK.matcher(value);
        StringBuffer placeholderBuf = new StringBuffer();
        int idx = 0;
        while (blockMatcher.find()) {
            blocks.add(blockMatcher.group());
            String placeholder = placeholderPrefix + idx + placeholderSuffix;
            blockMatcher.appendReplacement(placeholderBuf, Matcher.quoteReplacement(placeholder));
            idx++;
        }
        blockMatcher.appendTail(placeholderBuf);

        List<String> tokens = new ArrayList<>();
        for (String part : StringUtils.split(placeholderBuf.toString(), separator)) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 还原占位符（trim 不会破坏 __RANGE_PH_0__ 这种可见字符串）
            for (int i = 0; i < blocks.size(); i++) {
                trimmed = trimmed.replace(placeholderPrefix + i + placeholderSuffix, blocks.get(i));
            }
            tokens.add(trimmed);
        }
        return tokens;
    }

    private static boolean isInteger(String s) {
        return s.matches("-?\\d+");
    }

    private static boolean isSingleLetter(String s) {
        return s.length() == 1 && Character.isLetter(s.charAt(0));
    }
}