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

    /** 是否已内置金牌分镜 */
    public boolean isGold(String conceptId) {
        return GOLD_CONCEPTS.contains(normalizer.normalize(conceptId));
    }

    /**
     * 按概念获取分镜。金牌概念读 demos/{id}/storyboard.json；否则返回占位（异步教案会覆盖）。
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

    /** 未知概念的本地占位分镜（异步教案生成前的临时骨架） */
    private Storyboard placeholder(String conceptId, String rawConcept) {
        String title = rawConcept == null ? conceptId : rawConcept;
        List<Storyboard.Beat> beats = List.of(
            new Storyboard.Beat("旁白", "先想：不用「" + title + "」时哪里别扭？", "准备看对照", "开场：找别扭"),
            new Storyboard.Beat("角色", "用笨办法硬做", "又慢又乱", "对照：错误做法"),
            new Storyboard.Beat("角色", "换成聪明做法", "变简单、能复用", "对照：正确做法"),
            new Storyboard.Beat("旁白", "收成一句口诀", "记住差在哪里", "收束")
        );
        return new Storyboard(conceptId, title, beats);
    }
}
