package com.motiflab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 演示 HTML 缓存：优先读磁盘，未命中则从 classpath 金牌 demo 复制。
 * 关联：ConceptNormalizer（缓存键）、后续 MotifTutorService。
 */
public class DemoCache {

    private final Path cacheDir;
    private final String protocolVersion;
    private final ConceptNormalizer normalizer = new ConceptNormalizer();

    /** 创建缓存目录；protocolVersion 参与缓存键 */
    public DemoCache(Path cacheDir, String protocolVersion) {
        this.cacheDir = cacheDir;
        this.protocolVersion = protocolVersion;
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建 demo 缓存目录: " + cacheDir, e);
        }
    }

    /**
     * 解析演示 HTML：磁盘命中优先，否则从 classpath demos/{id}/L{n}.html 复制到磁盘。
     * conceptId 应为已归一化 id（如 loop）；内部仍经 cacheKey 规范化。
     */
    public Optional<Path> resolve(String conceptId, int level) {
        String fileName = toFileName(conceptId, level);
        Path diskPath = cacheDir.resolve(fileName);
        if (Files.isRegularFile(diskPath)) {
            return Optional.of(diskPath);
        }

        int clamped = normalizer.clampLevel(level);
        String normalized = normalizer.normalize(conceptId);
        String resource = "demos/" + normalized + "/L" + clamped + ".html";
        try (InputStream in = DemoCache.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return Optional.empty();
            }
            Files.copy(in, diskPath);
            return Optional.of(diskPath);
        } catch (IOException e) {
            throw new UncheckedIOException("复制 classpath demo 失败: " + resource, e);
        }
    }

    /** 将 HTML 写入磁盘缓存 */
    public void put(String conceptId, int level, String html) {
        Path diskPath = cacheDir.resolve(toFileName(conceptId, level));
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(diskPath, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写入 demo 缓存失败: " + diskPath, e);
        }
    }

    /** cacheKey 含 |，Windows 非法；落盘时替换为 _ */
    private String toFileName(String conceptId, int level) {
        return normalizer.cacheKey(conceptId, level, protocolVersion).replace('|', '_') + ".html";
    }
}
