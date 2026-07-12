package com.motiflab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motiflab.model.QuizItem;
import com.motiflab.model.Storyboard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 非金牌概念：用 LLM 一次生成真分镜 + 口诀 + 检验题。
 * 关联：LlmClient、MotifTutorService、prompts/teaching-pack.md。
 */
@Component
public class TeachingPackGenerator {

    /** 教案包：分镜、口诀、题 */
    public record Pack(Storyboard storyboard, String motto, List<QuizItem> quiz) {}

    private final LlmClient llm;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String systemPrompt;

    public TeachingPackGenerator(LlmClient llm) {
        this.llm = llm;
        this.systemPrompt = loadSystemPrompt();
    }

    /** 为概念生成教案包；解析失败抛 IllegalStateException */
    public Pack generate(String conceptRaw, String conceptId, int level, String sceneSeed) {
        String user = buildUserPrompt(conceptRaw, conceptId, level, sceneSeed);
        String raw = llm.complete(systemPrompt, user);
        return parse(conceptId, raw);
    }

    /** 解析模型 JSON；供测试直接调用 */
    Pack parse(String conceptId, String raw) {
        String json = AnimationGenerator.stripFences(raw);
        try {
            JsonNode root = mapper.readTree(json);
            String title = textOr(root, "title", conceptId);
            String motto = textOr(root, "motto", "");
            if (motto.isBlank() || motto.contains("先看懂故事")) {
                throw new IllegalStateException("教案口诀不合格");
            }
            List<Storyboard.Beat> beats = new ArrayList<>();
            JsonNode beatsNode = root.path("beats");
            if (!beatsNode.isArray() || beatsNode.size() < 3) {
                throw new IllegalStateException("教案分镜少于 3 拍");
            }
            for (JsonNode b : beatsNode) {
                beats.add(new Storyboard.Beat(
                        textOr(b, "who", "角色"),
                        textOr(b, "action", ""),
                        textOr(b, "result", ""),
                        textOr(b, "principle", "")
                ));
            }
            List<QuizItem> quiz = new ArrayList<>();
            JsonNode quizNode = root.path("quiz");
            if (!quizNode.isArray() || quizNode.size() < 2) {
                throw new IllegalStateException("教案检验题不足 2 道");
            }
            int i = 1;
            for (JsonNode q : quizNode) {
                List<String> choices = new ArrayList<>();
                JsonNode choicesNode = q.path("choices");
                if (choicesNode.isArray()) {
                    for (JsonNode c : choicesNode) {
                        choices.add(c.asText(""));
                    }
                }
                if (choices.size() < 2) {
                    throw new IllegalStateException("检验题选项不足");
                }
                String id = textOr(q, "id", "q" + i);
                String question = textOr(q, "question", "");
                int answerIndex = q.path("answerIndex").asInt(0);
                if (answerIndex < 0 || answerIndex >= choices.size()) {
                    throw new IllegalStateException("检验题答案下标非法");
                }
                if (question.contains("动画主要在讲什么")) {
                    throw new IllegalStateException("检验题过于空泛");
                }
                quiz.add(new QuizItem(id, question, List.copyOf(choices), answerIndex));
                i++;
            }
            return new Pack(
                    new Storyboard(conceptId, title, List.copyOf(beats)),
                    motto,
                    List.copyOf(quiz)
            );
        } catch (IOException e) {
            throw new IllegalStateException("教案 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    private static String buildUserPrompt(String conceptRaw, String conceptId, int level, String sceneSeed) {
        return "概念原文：" + conceptRaw + "\n"
                + "概念 id：" + conceptId + "\n"
                + "简版等级：L" + level + "\n"
                + "场景种子：" + (sceneSeed == null || sceneSeed.isBlank() ? "（无，自选具体故事）" : sceneSeed) + "\n"
                + "请直接输出 JSON。";
    }

    private static String textOr(JsonNode node, String field, String fallback) {
        JsonNode v = node.path(field);
        if (v.isMissingNode() || v.isNull()) {
            return fallback;
        }
        String t = v.asText("");
        return t.isBlank() ? fallback : t;
    }

    private static String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource("prompts/teaching-pack.md").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法加载 prompts/teaching-pack.md", e);
        }
    }
}
