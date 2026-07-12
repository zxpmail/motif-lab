package com.motiflab.service;

import java.util.Locale;

/** 概念名规范化与演示缓存键。关联：DemoCache、MotifTutorService。 */
public class ConceptNormalizer {
    /** 将用户输入的概念归一为稳定 id */
    public String normalize(String raw) {
        if (raw == null) return "unknown";
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.contains("循环") || s.contains("loop") || s.contains("for") || s.contains("while")) {
            return "loop";
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
