package com.motiflab.service;

import com.motiflab.model.Storyboard;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 分镜获取：有金牌资源的概念读 classpath JSON，其它返回试做占位。
 * 关联：ConceptNormalizer、Storyboard、MotifTutorService。
 */
public class StoryboardService {

    /** 已内置金牌分镜的概念 id */
    private static final Set<String> GOLD_CONCEPTS = Set.of("loop", "variable", "condition", "function");

    private final ConceptNormalizer normalizer;
    private final ObjectMapper mapper = new ObjectMapper();

    public StoryboardService(ConceptNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /**
     * 按概念获取分镜。金牌概念读 demos/{id}/storyboard.json；否则占位。
     */
    public Storyboard getOrCreate(String concept, int level) {
        String normalized = normalizer.normalize(concept);
        if (GOLD_CONCEPTS.contains(normalized)) {
            return loadGoldStoryboard(normalized);
        }
        return placeholder(normalized, concept);
    }

    /** 从 classpath 读取金牌分镜 JSON */
    private Storyboard loadGoldStoryboard(String conceptId) {
        String resource = "demos/" + conceptId + "/storyboard.json";
        try (InputStream in = StoryboardService.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("缺少 classpath 资源: " + resource);
            }
            JsonNode root = mapper.readTree(in);
            String title = root.path("title").asString(conceptId);
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
            return new Storyboard(conceptId, title, List.copyOf(beats));
        } catch (JacksonException | IOException e) {
            throw new IllegalStateException("读取 storyboard 失败: " + resource, e);
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
