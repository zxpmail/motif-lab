package com.motiflab.service;

import com.motiflab.dto.StartLessonRequest;
import com.motiflab.model.ContrastRow;
import com.motiflab.model.MotifSession;
import com.motiflab.model.QuizItem;
import com.motiflab.model.Storyboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 母题授课会话状态机。
 * 当前策略：只生成文字母题（寓言+对照+提炼+口诀+题），不生成动画 HTML。
 * 关联：ConceptNormalizer、StoryboardService、DemoCache（仅缓存教案 JSON）、TeachingPackGenerator。
 */
public class MotifTutorService {

    private static final String LOOP_MOTTO = "循环就是同一件事，按次数做多遍。";
    private static final String VARIABLE_MOTTO = "变量是贴了名字的盒子，里面的东西可以改。";
    private static final String CONDITION_MOTTO = "条件就是看情况，选一条该走的路。";
    private static final String FUNCTION_MOTTO = "函数像一台有名字的机器：放进输入，按固定步骤，吐出结果。";

    private final ConceptNormalizer normalizer;
    private final StoryboardService storyboards;
    private final DemoCache demos;
    /** 可空：非金牌母题生成 */
    private final TeachingPackGenerator teachingPacks;
    private final ConcurrentHashMap<String, MotifSession> sessions = new ConcurrentHashMap<>();

    public MotifTutorService(ConceptNormalizer normalizer, StoryboardService storyboards, DemoCache demos) {
        this(normalizer, storyboards, demos, null);
    }

    public MotifTutorService(
            ConceptNormalizer normalizer,
            StoryboardService storyboards,
            DemoCache demos,
            TeachingPackGenerator teachingPacks) {
        this.normalizer = normalizer;
        this.storyboards = storyboards;
        this.demos = demos;
        this.teachingPacks = teachingPacks;
    }

    /** 开始一课 */
    public MotifSession start(StartLessonRequest req) {
        String id = UUID.randomUUID().toString();
        MotifSession session = new MotifSession();
        session.setId(id);
        session.setConceptRaw(req.getConcept());
        session.setConceptId(normalizer.normalize(req.getConcept()));
        session.setLevel(0);
        session.setSceneSeed(null);
        session.setStoryboard(storyboards.getOrCreate(req.getConcept(), 0));
        session.setDemoUrl(null);
        applyLesson(session);
        sessions.put(id, session);
        return session;
    }

    /** 更简单一版寓言（升 L） */
    public MotifSession simplify(String sessionId) {
        MotifSession session = get(sessionId);
        resetProgressForRerun(session);
        session.setLevel(normalizer.clampLevel(session.getLevel() + 1));
        applyLesson(session);
        return session;
    }

    /** /重写：换场景种子，重新生成寓言 */
    public MotifSession rewrite(String sessionId) {
        MotifSession session = get(sessionId);
        resetProgressForRerun(session);
        session.setSceneSeed(UUID.randomUUID().toString());
        applyLesson(session);
        return session;
    }

    private void resetProgressForRerun(MotifSession session) {
        session.getCorrectQuizIds().clear();
        session.setGenerationToken(session.getGenerationToken() + 1);
        session.setMotto(null);
        session.setQuiz(null);
        session.setFable(null);
        session.setExplanation(null);
        session.setContrast(null);
        session.setMotif(null);
        session.setDemoUrl(null);
        session.setError(null);
    }

    public MotifSession get(String sessionId) {
        MotifSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        return session;
    }

    public boolean answer(String sessionId, String quizId, int choiceIndex) {
        MotifSession session = get(sessionId);
        List<QuizItem> quiz = session.getQuiz();
        if (quiz == null || quiz.isEmpty()) {
            throw new IllegalArgumentException("当前会话无检验题");
        }
        QuizItem item = quiz.stream()
            .filter(q -> quizId.equals(q.id()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("题目不存在: " + quizId));
        boolean correct = item.answerIndex() == choiceIndex;
        if (correct) {
            session.getCorrectQuizIds().add(quizId);
            if (session.getCorrectQuizIds().size() >= quiz.size()) {
                session.setPhase("DONE");
            }
        }
        return correct;
    }

    public record AnswerResult(boolean correct, MotifSession session) {}

    public AnswerResult answerResult(String id, String quizId, int index) {
        boolean correct = answer(id, quizId, index);
        return new AnswerResult(correct, get(id));
    }

    /**
     * 金牌：同步文字教案；非金牌：教案缓存命中则恢复，否则异步生成母题（不生成动画）。
     */
    private void applyLesson(MotifSession session) {
        session.setError(null);
        session.setDemoUrl(null);

        if (storyboards.isGold(session.getConceptId())) {
            fillGoldLesson(session);
            session.setPhase("MOTTO_QUIZ");
            return;
        }

        Optional<String> cached = demos.resolvePackJson(
                session.getConceptId(), session.getLevel(), session.getSceneSeed());
        if (cached.isPresent() && teachingPacks != null) {
            try {
                TeachingPackGenerator.Pack pack = teachingPacks.parse(session.getConceptId(), cached.get());
                applyPack(session, pack);
                session.setPhase("MOTTO_QUIZ");
                return;
            } catch (RuntimeException ignored) {
                // 旧格式缓存忽略，重新生成
            }
        }

        session.setPhase("DEMO");
        startAsyncFable(session);
    }

    /** 金牌概念：口诀题 + 由分镜拼成的短文案（不调 LLM、不播动画） */
    private void fillGoldLesson(MotifSession session) {
        fillMottoQuiz(session);
        Storyboard sb = session.getStoryboard();
        if (sb == null || sb.beats() == null || sb.beats().isEmpty()) {
            return;
        }
        StringBuilder fable = new StringBuilder();
        List<ContrastRow> contrast = new ArrayList<>();
        for (Storyboard.Beat beat : sb.beats()) {
            fable.append(beat.who()).append(beat.action())
                    .append("，于是").append(beat.result()).append("。");
            contrast.add(new ContrastRow(beat.action() + " → " + beat.result(), beat.principle()));
            if (contrast.size() >= 3) {
                break;
            }
        }
        session.setFable(fable.toString());
        session.setExplanation(session.getMotto());
        session.setContrast(List.copyOf(contrast));
        session.setMotif(session.getMotto() == null ? sb.title() : session.getMotto());
    }

    /** 异步只生成母题文字教案 */
    private void startAsyncFable(MotifSession session) {
        if (teachingPacks == null) {
            session.setError("请先在设置中配置并启用 LLM，才能生成母题寓言");
            return;
        }
        final String sessionId = session.getId();
        final String conceptRaw = session.getConceptRaw();
        final String conceptId = session.getConceptId();
        final int level = session.getLevel();
        final String sceneSeed = session.getSceneSeed();
        final long token = session.getGenerationToken();

        CompletableFuture.runAsync(() -> {
            try {
                TeachingPackGenerator.Pack pack =
                        teachingPacks.generate(conceptRaw, conceptId, level, sceneSeed);
                MotifSession live = sessions.get(sessionId);
                if (live == null || live.getGenerationToken() != token) {
                    return;
                }
                applyPack(live, pack);
                demos.putPackJson(conceptId, level, sceneSeed, toPackJson(pack));
                live.setPhase("MOTTO_QUIZ");
                live.setError(null);
                live.setDemoUrl(null);
            } catch (Exception e) {
                MotifSession live = sessions.get(sessionId);
                if (live != null && live.getGenerationToken() == token) {
                    String msg = e.getMessage();
                    live.setError(msg == null || msg.isBlank() ? "母题生成失败" : msg);
                }
            }
        });
    }

    private void applyPack(MotifSession session, TeachingPackGenerator.Pack pack) {
        session.setStoryboard(pack.storyboard());
        session.setFable(pack.fable());
        session.setExplanation(pack.explanation());
        session.setContrast(pack.contrast());
        session.setMotif(pack.motif());
        session.setMotto(pack.motto());
        session.setQuiz(pack.quiz());
        session.setDemoUrl(null);
    }

    private static String toPackJson(TeachingPackGenerator.Pack pack) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"title\":").append(jsonStr(pack.storyboard().title()));
        sb.append(",\"fable\":").append(jsonStr(pack.fable()));
        sb.append(",\"explanation\":").append(jsonStr(pack.explanation()));
        sb.append(",\"motif\":").append(jsonStr(pack.motif()));
        sb.append(",\"motto\":").append(jsonStr(pack.motto()));
        sb.append(",\"contrast\":[");
        List<ContrastRow> contrast = pack.contrast();
        for (int i = 0; i < contrast.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            ContrastRow row = contrast.get(i);
            sb.append("{\"story\":").append(jsonStr(row.story()))
                    .append(",\"concept\":").append(jsonStr(row.concept())).append('}');
        }
        sb.append("],\"quiz\":[");
        List<QuizItem> quiz = pack.quiz();
        for (int i = 0; i < quiz.size(); i++) {
            QuizItem q = quiz.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"id\":").append(jsonStr(q.id()))
                    .append(",\"question\":").append(jsonStr(q.question()))
                    .append(",\"choices\":[");
            for (int j = 0; j < q.choices().size(); j++) {
                if (j > 0) {
                    sb.append(',');
                }
                sb.append(jsonStr(q.choices().get(j)));
            }
            sb.append("],\"answerIndex\":").append(q.answerIndex()).append('}');
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String jsonStr(String s) {
        if (s == null) {
            return "\"\"";
        }
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "") + "\"";
    }

    private void fillMottoQuiz(MotifSession session) {
        String id = session.getConceptId();
        if ("loop".equals(id)) {
            session.setMotto(LOOP_MOTTO);
            session.setQuiz(List.of(
                new QuizItem("q1", "循环在做什么？",
                    List.of("只做一次", "同样动作做多遍", "随便做"), 1),
                new QuizItem("q2", "上面叠积木叠了几次？",
                    List.of("1", "3", "10"), 1)
            ));
            return;
        }
        if ("variable".equals(id)) {
            session.setMotto(VARIABLE_MOTTO);
            session.setQuiz(List.of(
                new QuizItem("q1", "变量最像什么？",
                    List.of("永远不能改的石头", "贴了名字、里面能换东西的盒子", "一把锁"), 1),
                new QuizItem("q2", "苹果数从 3 变成 5，说明什么？",
                    List.of("名字也必须改成新名字", "名字可以不变，值可以变", "盒子坏了"), 1)
            ));
            return;
        }
        if ("condition".equals(id)) {
            session.setMotto(CONDITION_MOTTO);
            session.setQuiz(List.of(
                new QuizItem("q1", "条件在帮你做什么？",
                    List.of("把所有路都走一遍", "根据情况选一条路", "永远只走左边"), 1),
                new QuizItem("q2", "下雨时该走哪条？",
                    List.of("阳光路", "伞路", "原地不动"), 1)
            ));
            return;
        }
        if ("function".equals(id)) {
            session.setMotto(FUNCTION_MOTTO);
            session.setQuiz(List.of(
                new QuizItem("q1", "函数最像什么？",
                    List.of("随便猜结果的盒子", "有名字的机器：进料→固定步骤→出结果", "只能用一次的火柴"), 1),
                new QuizItem("q2", "问好机里放进「小红」，会吐出什么？",
                    List.of("你好，小明", "你好，小红", "随便一句话"), 1)
            ));
        }
    }
}
