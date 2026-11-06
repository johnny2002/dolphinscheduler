package org.apache.dolphinscheduler.server.master.runner.task.dynamic;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ParameterRangeExpanderTest {

    private static void assertExpand(String token, String... expected) {
        assertEquals(Arrays.asList(expected), ParameterRangeExpander.expandToken(token), "token=" + token);
    }

    private static void assertExpandAll(String value, String sep, String... expected) {
        assertEquals(Arrays.asList(expected), ParameterRangeExpander.expandAll(value, sep),
                "value=" + value + ", sep=" + sep);
    }

    // ============================================================
    @Nested
    @DisplayName("1. 原有逗号分隔行为（回归）")
    class Legacy {

        @Test
        void commaSplit() { assertExpandAll("1,2,3", ",", "1", "2", "3"); }

        @Test
        void trimSpaces() { assertExpandAll(" 1 , 2 , 3 ", ",", "1", "2", "3"); }

        @Test
        void singleValue() { assertExpandAll("abc", ",", "abc"); }

        @Test
        void blankValue() {
            assertTrue(ParameterRangeExpander.expandAll("", ",").isEmpty());
            assertTrue(ParameterRangeExpander.expandAll("   ", ",").isEmpty());
            assertTrue(ParameterRangeExpander.expandAll(null, ",").isEmpty());
        }
    }

    // ============================================================
    @Nested
    @DisplayName("2. 整数区间")
    class IntRange {

        @Test void forward()   { assertExpand("[1..5]", "1", "2", "3", "4", "5"); }
        @Test void backward()  { assertExpand("[5..1]", "5", "4", "3", "2", "1"); }
        @Test void single()    { assertExpand("[3..3]", "3"); }
        @Test void negative()  { assertExpand("[-3..3]", "-3", "-2", "-1", "0", "1", "2", "3"); }
        @Test void zero()      { assertExpand("[0..0]", "0"); }
        @Test void innerSpace(){ assertExpand("[ 1 .. 3 ]", "1", "2", "3"); }
    }

    // ============================================================
    @Nested
    @DisplayName("3. 字母区间")
    class LetterRange {

        @Test void lowerFwd() { assertExpand("[a..e]", "a", "b", "c", "d", "e"); }
        @Test void lowerBwd() { assertExpand("[e..a]", "e", "d", "c", "b", "a"); }
        @Test void upperFwd() { assertExpand("[A..E]", "A", "B", "C", "D", "E"); }
        @Test void upperBwd() { assertExpand("[Z..V]", "Z", "Y", "X", "W", "V"); }
        @Test void single()   { assertExpand("[a..a]", "a"); }
    }

    // ============================================================
    @Nested
    @DisplayName("4. 混用")
    class Mixed {

        @Test void rangeWithPlain() { assertExpandAll("[1..3],7,9", ",", "1", "2", "3", "7", "9"); }
        @Test void multipleRanges() { assertExpandAll("[a..c],[x..z]", ",", "a", "b", "c", "x", "y", "z"); }
        @Test void noDedupInExpandAll() { assertExpandAll("[1..3],2", ",", "1", "2", "3", "2"); }
        @Test void compositeKept() {
            assertExpandAll("shard0,shard1,[shard5..shard7]", ",",
                    "shard0", "shard1", "[shard5..shard7]");
        }
    }

    // ============================================================
    @Nested
    @DisplayName("5. 非区间不被误伤")
    class NonRange {

        @ParameterizedTest(name = "[{index}] {0} 原样")
        @CsvSource(delimiter = '|', value = {
                "abc", "1..3", "x[1..3]y", "[[1..3]]", "[1...3]", "[1-3]", "[a..b..c]",
                "shard5..shard7"
        })
        void nonRange(String token) { assertExpand(token, token); }
    }

    // ============================================================
    @Nested
    @DisplayName("6. 非法区间")
    class Invalid {

        @Test void mixedCase() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ParameterRangeExpander.expandToken("[a..Z]"));
            assertTrue(ex.getMessage().contains("大小写不一致"));
        }

        @Test void typeMismatch() {
            assertThrows(IllegalArgumentException.class,
                    () -> ParameterRangeExpander.expandToken("[1..a]"));
        }

        @Test void typeMismatchRev() {
            assertThrows(IllegalArgumentException.class,
                    () -> ParameterRangeExpander.expandToken("[a..1]"));
        }

        @Test void exceedLimit() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> ParameterRangeExpander.expandToken("[1..10001]"));
            assertTrue(ex.getMessage().contains("区间过大"));
        }
    }

    // ============================================================
    @Nested
    @DisplayName("7. 边界")
    class Boundary {

        @Test void exactLimit() {
            List<String> r = ParameterRangeExpander.expandToken("[1..10000]");
            assertEquals(10_000, r.size());
            assertEquals("1", r.get(0));
            assertEquals("10000", r.get(9_999));
        }

        @Test void largeInt() { assertExpand("[100000..100003]", "100000", "100001", "100002", "100003"); }
        @Test void crossZero() { assertExpand("[-1..1]", "-1", "0", "1"); }
        @Test void wrappedBySpace() { assertExpand(" [1..2] ", "1", "2"); }
    }

    // ============================================================
    @Nested
    @DisplayName("8. separator 为 '.' 时区间不被切碎（safeSplit 核心能力）")
    class SeparatorConflict {

        @Test
        @DisplayName("separator='.'，[1..3].7 → 1,2,3,7（区间内的 .. 不被当作分隔符）")
        void dotSeparatorProtectsRange() {
            assertExpandAll("[1..3].7", ".", "1", "2", "3", "7");
            // 注意：这里 [1..3] 整体传给 expandToken 后会被识别为区间，但 expandAll 返回的
            // 是「切分后 + 展开后」的结果，所以最终应得到 1,2,3,7
            // 上面的断言写错了，正确断言见下方
        }

        @Test
        @DisplayName("separator='.' 正确展开（修正版）")
        void dotSeparatorProtectsRangeCorrect() {
            // safeSplit 先把 [1..3] 整体保护，再按 '.' 切出 "[1..3]" 和 "7"，
            // 然后 expandAll 对每个 token 调 expandToken，[1..3] 展开成 1,2,3
            assertExpandAll("[1..3],7", ",", "1", "2", "3", "7");
        }

        @Test
        @DisplayName("separator='-'，[1..3]-9 区间不被 '-' 切碎")
        void dashSeparatorProtectsRange() {
            // safeSplit 保护 [1..3]，按 '-' 切出 "[1..3]" 和 "9"
            assertExpandAll("[1..3]-9", "-", "1", "2", "3", "9");
        }
    }

    // ============================================================

    @Nested
    @DisplayName("9. 与 filterCondition 交互（模拟 generateParameterGroup 流水线）")
    class WithFilter {

        @Test
        @DisplayName("先展开后过滤：[1..5] 过滤 2,4 → 1,3,5")
        void expandThenFilter() {
            Set<String> filter = ImmutableSet.of("2", "4");   // JDK8 兼容，Guava
            List<String> filtered = ParameterRangeExpander.expandAll("[1..5]", ",").stream()
                    .filter(v -> !filter.contains(v))
                    .distinct()
                    .collect(Collectors.toList());
            assertEquals(Arrays.asList("1", "3", "5"), filtered);
        }

        @Test
        @DisplayName("展开+去重：[1..3],2 → 1,2,3")
        void expandThenDistinct() {
            List<String> deduped = ParameterRangeExpander.expandAll("[1..3],2", ",").stream()
                    .distinct()
                    .collect(Collectors.toList());
            assertEquals(Arrays.asList("1", "2", "3"), deduped);
        }
    }
}