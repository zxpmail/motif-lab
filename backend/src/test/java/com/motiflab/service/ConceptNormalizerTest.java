package com.motiflab.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConceptNormalizerTest {
    @Test
    void normalize_loopAliases() {
        ConceptNormalizer n = new ConceptNormalizer();
        assertEquals("loop", n.normalize("循环"));
        assertEquals("loop", n.normalize(" for 循环 "));
        assertEquals("loop", n.normalize("LOOP"));
        assertEquals("loop", n.normalize("while循环"));
    }

    @Test
    void normalize_variableAndCondition() {
        ConceptNormalizer n = new ConceptNormalizer();
        assertEquals("variable", n.normalize("变量"));
        assertEquals("variable", n.normalize("Variable"));
        assertEquals("condition", n.normalize("条件判断"));
        assertEquals("condition", n.normalize("if语句"));
    }

    @Test
    void cacheKey_includesLevelAndProtocol() {
        ConceptNormalizer n = new ConceptNormalizer();
        assertEquals("loop|L0|v1", n.cacheKey("循环", 0, "v1"));
        assertEquals("variable|L1|v1", n.cacheKey("变量", 1, "v1"));
    }

    @Test
    void clampLevel() {
        ConceptNormalizer n = new ConceptNormalizer();
        assertEquals(0, n.clampLevel(-1));
        assertEquals(2, n.clampLevel(99));
    }
}
