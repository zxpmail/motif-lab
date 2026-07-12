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
}
