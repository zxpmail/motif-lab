package com.motiflab.service;

import com.motiflab.model.Storyboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StoryboardServiceTest {

    @Test
    void loop_usesClasspathJson_fast() {
        StoryboardService svc = new StoryboardService(new ConceptNormalizer());
        long t0 = System.nanoTime();
        Storyboard sb = svc.getOrCreate("循环", 0);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertEquals("loop", sb.conceptId());
        assertTrue(sb.beats().size() >= 3);
        assertTrue(ms < 500, "local storyboard must be fast, was " + ms);
    }

    @Test
    void unknown_returnsPlaceholderFast() {
        StoryboardService svc = new StoryboardService(new ConceptNormalizer());
        Storyboard sb = svc.getOrCreate("量子纠缠", 0);
        assertNotNull(sb.title());
        assertTrue(sb.beats().size() >= 1);
    }

    @Test
    void variable_and_condition_useGoldStoryboard() {
        StoryboardService svc = new StoryboardService(new ConceptNormalizer());
        Storyboard v = svc.getOrCreate("变量", 0);
        assertEquals("variable", v.conceptId());
        assertTrue(v.beats().size() >= 3);
        Storyboard c = svc.getOrCreate("条件", 0);
        assertEquals("condition", c.conceptId());
        assertTrue(c.beats().size() >= 3);
    }
}
