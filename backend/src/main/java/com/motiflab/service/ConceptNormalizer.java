package com.motiflab.service;

import java.util.Locale;

/** 概念名规范化与演示缓存键。关联：DemoCache、MotifTutorService。 */
public class ConceptNormalizer {

    /** 将用户输入的概念归一为稳定 id（编程金牌：loop / variable / condition） */
    public String normalize(String raw) {
        if (raw == null) {
            return "unknown";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        // 循环：避免单独匹配 for（会误伤 transform 等英文词）
        if (s.contains("循环") || s.contains("loop") || s.contains("while")
                || s.contains("for循环") || s.contains("for loop") || s.contains("for-loop")) {
            return "loop";
        }
        if (s.contains("变量") || s.contains("variable") || s.equals("var") || s.contains("赋值")) {
            return "variable";
        }
        if (s.contains("条件") || s.contains("判断") || s.contains("分支")
                || s.contains("condition") || s.contains("else") || s.contains("if语句")
                || s.matches(".*\\bif\\b.*")) {
            return "condition";
        }
        return s.replaceAll("[^a-z0-9\\u4e00-\\u9fff]+", "-");
    }

    /** 将简版等级限制在 0..2 */
    public int clampLevel(int level) {
        return Math.max(0, Math.min(2, level));
    }

    /** 生成 demo 缓存键：conceptId|L{n}|protocol */
    public String cacheKey(String concept, int level, String protocolVersion) {
        return normalize(concept) + "|L" + clampLevel(level) + "|" + protocolVersion;
    }
}
