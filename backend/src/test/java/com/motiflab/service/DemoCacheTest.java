package com.motiflab.service;

import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class DemoCacheTest {
    @Test
    void resolve_prefersClasspathGoldThenDisk() throws Exception {
        Path dir = Files.createTempDirectory("demo-cache");
        DemoCache cache = new DemoCache(dir, "v1");
        Optional<Path> hit = cache.resolve("loop", 0);
        assertTrue(hit.isPresent());
        assertTrue(Files.readString(hit.get()).contains("motif-lab gold"));
    }

    @Test
    void put_then_resolve_readsDisk() throws Exception {
        Path dir = Files.createTempDirectory("demo-cache");
        DemoCache cache = new DemoCache(dir, "v1");
        cache.put("custom", 0, "<!-- motif-lab gold: custom -->\n<html>ok</html>");
        Optional<Path> hit = cache.resolve("custom", 0);
        assertTrue(hit.isPresent());
        assertTrue(Files.readString(hit.get()).contains("custom"));
    }

    @Test
    void resolve_missing_returnsEmpty() throws Exception {
        Path dir = Files.createTempDirectory("demo-cache");
        DemoCache cache = new DemoCache(dir, "v1");
        assertTrue(cache.resolve("nope", 0).isEmpty());
    }
}
