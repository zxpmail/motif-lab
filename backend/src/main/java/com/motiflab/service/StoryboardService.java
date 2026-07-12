package com.motiflab.service;

import com.motiflab.model.Storyboard;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 分镜获取：loop 读本地 classpath JSON，其它概念返回试做占位（无 LLM，保证小于 500ms）。
 * 关联：ConceptNormalizer、Storyboard、后续 MotifTutorService。
 */
public class StoryboardService {

    private static final String LOOP_RESOURCE = "demos/loop/storyboard.json";

    private final ConceptNormalizer normalizer;
    private final ObjectMapper mapper = new ObjectMapper();

    /** 注入概念规范化器；level 在 v0.1 对 loop 暂不区分分镜 */
    public StoryboardService(ConceptNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * 按概念获取或生成分镜。normalize==loop 时读本地种子；否则返回试做占位。
     * @param concept 原始概念文案
     * @param level   简版等级（v0.1 loop 共用同一分镜，可未使用）
     */
    public Storyboard getOrCreate(String concept, int level) {
        String normalized = normalizer.normalize(concept);
        if ("loop".equals(normalized)) {
            return loadLoopStoryboard();
        }
        return placeholder(normalized, concept);
    }

    /** 从 classpath 读取 loop 金牌分镜 JSON */
    private Storyboard loadLoopStoryboard() {
        try (InputStream in = StoryboardService.class.getClassLoader().getResourceAsStream(LOOP_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("缺少 classpath 资源: " + LOOP_RESOURCE);
            }
            JsonNode root = mapper.readTree(in);
            String title = root.path("title").asString("循环");
            List<Storyboard.Beat> beats = new ArrayList<>();
            JsonNode beatsNode = root.path("beats");
            if (beatsNode.isArray()) {
                for (JsonNode b : beatsNode) {
                    beats.add(new Storyboard.Beat(
                        b.path("who").asString(""),
                        b.path("action").asString(""),
                        b.path("result").asString(""),
                        b.path("principle").asString("")
                    ));
                }
            }
            return new Storyboard("loop", title, List.copyOf(beats));
        } catch (JacksonException | IOException e) {
            throw new IllegalStateException("读取 loop storyboard 失败", e);
        }
    }

    /** 未知概念的本地占位分镜（标明试做，不调用 LLM） */
    private Storyboard placeholder(String conceptId, String rawConcept) {
        String title = rawConcept == null ? conceptId : rawConcept;
        List<Storyboard.Beat> beats = List.of(
            new Storyboard.Beat("旁白", "引出概念「" + title + "」", "听者知道要学什么", "试做：开场"),
            new Storyboard.Beat("角色", "做一次关键动作", "出现可见结果", "试做：演示核心"),
            new Storyboard.Beat("旁白", "用一句话收束", "记住口诀要点", "试做：收束")
        );
        return new Storyboard(conceptId, title, beats);
    }
}
