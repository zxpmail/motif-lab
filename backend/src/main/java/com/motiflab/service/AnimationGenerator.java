package com.motiflab.service;

import com.motiflab.model.Storyboard;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 根据分镜调用 LLM 生成单文件教学动画 HTML。
 * 关联：LlmClient、prompts/animation-gen.md、MotifTutorService。
 */
@Component
public class AnimationGenerator {

    private final LlmClient llm;
    private final String systemPrompt;

    public AnimationGenerator(LlmClient llm) {
        this.llm = llm;
        this.systemPrompt = loadSystemPrompt();
    }

    /**
     * 生成完整 HTML；校验含 {@code <html}。
     * @throws IllegalStateException 调用失败或输出非法
     */
    public String generate(Storyboard board, int level, String sceneSeed) {
        String user = buildUserPrompt(board, level, sceneSeed);
        String raw = llm.complete(systemPrompt, user);
        String html = stripFences(raw);
        if (html == null || !html.toLowerCase().contains("<html")) {
            throw new IllegalStateException("LLM 输出不是合法 HTML（缺少 <html）");
        }
        return html;
    }

    /** 从 beats + level 约束 + sceneSeed 拼 user 消息 */
    private String buildUserPrompt(Storyboard board, int level, String sceneSeed) {
        StringBuilder sb = new StringBuilder();
        sb.append("概念 id：").append(board == null ? "" : board.conceptId()).append('\n');
        sb.append("标题：").append(board == null ? "" : board.title()).append('\n');
        sb.append("简版等级：L").append(level).append('\n');
        sb.append("等级约束：").append(levelConstraint(level)).append('\n');
        sb.append("场景种子 sceneSeed：").append(sceneSeed == null ? "（无）" : sceneSeed).append('\n');
        sb.append("分镜节拍：\n");
        if (board != null && board.beats() != null) {
            int i = 1;
            for (Storyboard.Beat beat : board.beats()) {
                sb.append(i++).append(". who=").append(beat.who())
                        .append(" | action=").append(beat.action())
                        .append(" | result=").append(beat.result())
                        .append(" | principle=").append(beat.principle())
                        .append('\n');
            }
        }
        sb.append("\n请直接输出完整 HTML。");
        return sb.toString();
    }

    private static String levelConstraint(int level) {
        return switch (level) {
            case 0 -> "L0：最少步骤、最大字、最慢动作；只讲一件事。";
            case 1 -> "L1：稍多一步细节，仍用大字和慢动作。";
            default -> "L2：可稍完整，但仍五岁能懂，不要术语堆砌。";
        };
    }

    /** 去掉模型可能包上的 markdown 代码围栏 */
    static String stripFences(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNl = s.indexOf('\n');
            if (firstNl > 0) {
                s = s.substring(firstNl + 1);
            }
            int end = s.lastIndexOf("```");
            if (end >= 0) {
                s = s.substring(0, end);
            }
            s = s.trim();
        }
        return s;
    }

    private static String loadSystemPrompt() {
        try (InputStream in = new ClassPathResource("prompts/animation-gen.md").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("无法加载 prompts/animation-gen.md", e);
        }
    }
}
