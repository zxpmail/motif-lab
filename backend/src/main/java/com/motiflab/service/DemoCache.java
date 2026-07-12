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
 * 可选 sceneSeed 参与键，使「换个故事」真正换缓存。
 * 关联：ConceptNormalizer、MotifTutorService。
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
     * sceneSeed 非空时不走金牌 classpath（避免换故事仍播旧金牌）。
     */
    public Optional<Path> resolve(String conceptId, int level, String sceneSeed) {
        String fileName = toFileName(conceptId, level, sceneSeed);
        Path diskPath = cacheDir.resolve(fileName);
        if (Files.isRegularFile(diskPath)) {
            return Optional.of(diskPath);
        }

        // 换故事种子存在时，不回退金牌文件
        if (sceneSeed != null && !sceneSeed.isBlank()) {
            return Optional.empty();
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

    /** 兼容旧调用：无 sceneSeed */
    public Optional<Path> resolve(String conceptId, int level) {
        return resolve(conceptId, level, null);
    }

    /** 将 HTML 写入磁盘缓存 */
    public void put(String conceptId, int level, String sceneSeed, String html) {
        Path diskPath = cacheDir.resolve(toFileName(conceptId, level, sceneSeed));
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(diskPath, html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写入 demo 缓存失败: " + diskPath, e);
        }
    }

    /** 兼容旧调用：无 sceneSeed */
    public void put(String conceptId, int level, String html) {
        put(conceptId, level, null, html);
    }

    /** 写入教案 sidecar（分镜/口诀/题的 JSON 原文） */
    public void putPackJson(String conceptId, int level, String sceneSeed, String json) {
        Path diskPath = cacheDir.resolve(toPackFileName(conceptId, level, sceneSeed));
        try {
            Files.createDirectories(cacheDir);
            Files.writeString(diskPath, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("写入教案缓存失败: " + diskPath, e);
        }
    }

    /** 读取教案 sidecar */
    public Optional<String> resolvePackJson(String conceptId, int level, String sceneSeed) {
        Path diskPath = cacheDir.resolve(toPackFileName(conceptId, level, sceneSeed));
        if (!Files.isRegularFile(diskPath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(diskPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("读取教案缓存失败: " + diskPath, e);
        }
    }

    /** cacheKey 含 |，Windows 非法；落盘时替换为 _ */
    private String toFileName(String conceptId, int level, String sceneSeed) {
        return normalizer.cacheKey(conceptId, level, protocolVersion, sceneSeed).replace('|', '_') + ".html";
    }

    private String toPackFileName(String conceptId, int level, String sceneSeed) {
        return normalizer.cacheKey(conceptId, level, protocolVersion, sceneSeed).replace('|', '_') + ".pack.json";
    }
}
