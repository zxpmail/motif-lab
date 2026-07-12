package com.motiflab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.motiflab.model.ContrastRow;
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
 * 用 LLM 生成母题文字教案（寓言+对照+提炼+口诀+题），不生成动画。
 * 关联：LlmClient、MotifTutorService、prompts/teaching-pack.md。
 */
@Component
public class TeachingPackGenerator {

    /** 母题教案包 */
    public record Pack(
            Storyboard storyboard,
            String fable,
            String explanation,
            List<ContrastRow> contrast,
            String motif,
            String motto,
            List<QuizItem> quiz
    ) {}

    private final LlmClient llm;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String systemPrompt;

    public TeachingPackGenerator(LlmClient llm) {
        this.llm = llm;
        this.systemPrompt = loadSystemPrompt();
    }

    /** 为概念生成母题教案；解析失败抛 IllegalStateException */
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
            String fable = textOr(root, "fable", "");
            String explanation = textOr(root, "explanation", "");
            String motif = textOr(root, "motif", "");
            String motto = textOr(root, "motto", "");
            if (fable.isBlank() || fable.length() < 80) {
                throw new IllegalStateException("母题寓言过短或缺失");
            }
            if (motif.isBlank()) {
                throw new IllegalStateException("母题提炼缺失");
            }
            motif = normalizeMotif(motif);
            if (motto.isBlank() || motto.contains("先看懂故事")) {
                throw new IllegalStateException("口诀不合格");
            }

            List<ContrastRow> contrast = new ArrayList<>();
            JsonNode contrastNode = root.path("contrast");
            if (contrastNode.isArray()) {
                for (JsonNode row : contrastNode) {
                    contrast.add(new ContrastRow(
                            textOr(row, "story", ""),
                            textOr(row, "concept", "")
                    ));
                    if (contrast.size() >= 3) {
                        break;
                    }
                }
            }
            if (contrast.isEmpty()) {
                throw new IllegalStateException("缺少对照表");
            }

            // 用对照表生成轻量 beats，供兼容旧 UI；主内容是寓言
            List<Storyboard.Beat> beats = new ArrayList<>();
            for (ContrastRow row : contrast) {
                beats.add(new Storyboard.Beat("对照", row.story(), row.concept(), motif));
            }

            List<QuizItem> quiz = new ArrayList<>();
            JsonNode quizNode = root.path("quiz");
            if (!quizNode.isArray() || quizNode.size() < 2) {
                throw new IllegalStateException("检验题不足 2 道");
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
                    fable,
                    explanation,
                    List.copyOf(contrast),
                    motif,
                    motto,
                    List.copyOf(quiz)
            );
        } catch (IOException e) {
            throw new IllegalStateException("教案 JSON 解析失败: " + e.getMessage(), e);
        }
    }

    /** 统一箭头写法；没有箭头但够长也保留原文 */
    private static String normalizeMotif(String motif) {
        String m = motif.trim()
                .replace("->", "→")
                .replace("=>", "→")
                .replace("⇒", "→");
        if (!m.contains("→") && m.length() >= 8) {
            // 模型常写成「A 多了，B 少了」——仍可用
            return m;
        }
        if (!m.contains("→")) {
            throw new IllegalStateException("母题提炼不合格（需要能看出 X 与 Y 的对照）");
        }
        return m;
    }

    private static String buildUserPrompt(String conceptRaw, String conceptId, int level, String sceneSeed) {
        String levelHint = switch (level) {
            case 1 -> "更简单：人物更少、悖论更直白、句子更短。";
            case 2 -> "极简：最短寓言，只留一个最狠的对照。";
            default -> "标准母题深度。";
        };
        return "概念原文：" + conceptRaw + "\n"
                + "概念 id：" + conceptId + "\n"
                + "简版等级：L" + level + "（" + levelHint + "）\n"
                + "场景种子：" + (sceneSeed == null || sceneSeed.isBlank() ? "（无，自选完全不同的生活场景）" : sceneSeed) + "\n"
                + "请直接输出 JSON。不要输出 HTML。";
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
