package com.motiflab.service;

import com.motiflab.dto.StartLessonRequest;
import com.motiflab.model.MotifSession;
import com.motiflab.model.QuizItem;
import com.motiflab.model.Storyboard;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 母题授课会话状态机。
 * 金牌缓存命中同步进 MOTTO_QUIZ；未命中则异步：教案包 → 动画。
 * 关联：ConceptNormalizer、StoryboardService、DemoCache、AnimationGenerator、TeachingPackGenerator。
 */
public class MotifTutorService {

    private static final String LOOP_MOTTO = "循环就是同一件事，按次数做多遍。";
    private static final String VARIABLE_MOTTO = "变量是贴了名字的盒子，里面的东西可以改。";
    private static final String CONDITION_MOTTO = "条件就是看情况，选一条该走的路。";
    private static final String FUNCTION_MOTTO = "函数像一台有名字的机器：放进输入，按固定步骤，吐出结果。";

    private final ConceptNormalizer normalizer;
    private final StoryboardService storyboards;
    private final DemoCache demos;
    /** 可空：单元测试或不启用生成时传 null */
    private final AnimationGenerator generator;
    /** 可空：非金牌教案生成 */
    private final TeachingPackGenerator teachingPacks;
    private final ConcurrentHashMap<String, MotifSession> sessions = new ConcurrentHashMap<>();

    public MotifTutorService(ConceptNormalizer normalizer, StoryboardService storyboards, DemoCache demos) {
        this(normalizer, storyboards, demos, null, null);
    }

    public MotifTutorService(
            ConceptNormalizer normalizer,
            StoryboardService storyboards,
            DemoCache demos,
            AnimationGenerator generator) {
        this(normalizer, storyboards, demos, generator, null);
    }

    public MotifTutorService(
            ConceptNormalizer normalizer,
            StoryboardService storyboards,
            DemoCache demos,
            AnimationGenerator generator,
            TeachingPackGenerator teachingPacks) {
        this.normalizer = normalizer;
        this.storyboards = storyboards;
        this.demos = demos;
        this.generator = generator;
        this.teachingPacks = teachingPacks;
    }

    /** 开始一课：建会话、取分镜、解析金牌 demo 或异步生成 */
    public MotifSession start(StartLessonRequest req) {
        String id = UUID.randomUUID().toString();
        MotifSession session = new MotifSession();
        session.setId(id);
        session.setConceptRaw(req.getConcept());
        session.setConceptId(normalizer.normalize(req.getConcept()));
        session.setLevel(0);
        session.setSceneSeed(null);
        session.setStoryboard(storyboards.getOrCreate(req.getConcept(), 0));
        applyDemoAndPhase(session);
        fillMottoQuizIfReady(session);
        sessions.put(id, session);
        return session;
    }

    /** 升简版等级并换 demo */
    public MotifSession simplify(String sessionId) {
        MotifSession session = get(sessionId);
        resetProgressForRerun(session);
        session.setLevel(normalizer.clampLevel(session.getLevel() + 1));
        applyDemoAndPhase(session);
        fillMottoQuizIfReady(session);
        return session;
    }

    /** 重写：换 sceneSeed，轻微改分镜 who，再解析 demo */
    public MotifSession rewrite(String sessionId) {
        MotifSession session = get(sessionId);
        resetProgressForRerun(session);
        session.setSceneSeed(UUID.randomUUID().toString());
        session.setStoryboard(renameFirstWho(session.getStoryboard(), "小红"));
        applyDemoAndPhase(session);
        fillMottoQuizIfReady(session);
        return session;
    }

    /** 再学一档/换故事前清空答题进度，并作废进行中的异步生成 */
    private void resetProgressForRerun(MotifSession session) {
        session.getCorrectQuizIds().clear();
        session.setGenerationToken(session.getGenerationToken() + 1);
        session.setMotto(null);
        session.setQuiz(null);
    }

    /** 按 id 取会话；不存在则抛 IllegalArgumentException */
    public MotifSession get(String sessionId) {
        MotifSession session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("会话不存在: " + sessionId);
        }
        return session;
    }

    /**
     * 答题：正确则记入 correctQuizIds；全部答对后 phase=DONE。
     * @return 本题是否答对
     */
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

    /** 答题结果：是否答对 + 更新后的会话 */
    public record AnswerResult(boolean correct, MotifSession session) {}

    /** 答题并返回完整结果（供 HTTP 层包装） */
    public AnswerResult answerResult(String id, String quizId, int index) {
        boolean correct = answer(id, quizId, index);
        return new AnswerResult(correct, get(id));
    }

    /**
     * 解析 demo：金牌/缓存命中 → URL + MOTTO_QUIZ；未命中 → DEMO + 异步生成。
     */
    private void applyDemoAndPhase(MotifSession session) {
        session.setError(null);
        Optional<Path> hit = demos.resolve(session.getConceptId(), session.getLevel(), session.getSceneSeed());
        if (hit.isPresent()) {
            // 非金牌若有教案缓存，恢复分镜口诀题，避免仍显示空壳占位
            restorePackIfPresent(session);
            session.setDemoUrl("/api/demos/" + session.getId());
            session.setPhase("MOTTO_QUIZ");
        } else {
            session.setDemoUrl(null);
            session.setPhase("DEMO");
            startAsyncGeneration(session);
        }
    }

    /** 从 sidecar 恢复教案；没有则忽略 */
    private void restorePackIfPresent(MotifSession session) {
        if (storyboards.isGold(session.getConceptId()) || teachingPacks == null) {
            return;
        }
        Optional<String> json = demos.resolvePackJson(
                session.getConceptId(), session.getLevel(), session.getSceneSeed());
        if (json.isEmpty()) {
            return;
        }
        try {
            TeachingPackGenerator.Pack pack = teachingPacks.parse(session.getConceptId(), json.get());
            applyPack(session, pack);
        } catch (RuntimeException ignored) {
            // 旧缓存教案不合格时忽略，仍可播 HTML
        }
    }

    /** 缓存未命中时后台：非金牌先教案，再生成 HTML */
    private void startAsyncGeneration(MotifSession session) {
        if (generator == null) {
            session.setError("动画生成器未就绪");
            return;
        }
        final String sessionId = session.getId();
        final String conceptRaw = session.getConceptRaw();
        final String conceptId = session.getConceptId();
        final int level = session.getLevel();
        final String sceneSeed = session.getSceneSeed();
        final long token = session.getGenerationToken();
        final boolean gold = storyboards.isGold(conceptId);

        CompletableFuture.runAsync(() -> {
            try {
                MotifSession live = sessions.get(sessionId);
                if (live == null || live.getGenerationToken() != token) {
                    return;
                }

                Storyboard board = live.getStoryboard();
                if (!gold) {
                    if (teachingPacks == null) {
                        live.setError("教案生成器未就绪");
                        return;
                    }
                    TeachingPackGenerator.Pack pack =
                            teachingPacks.generate(conceptRaw, conceptId, level, sceneSeed);
                    if (live.getGenerationToken() != token) {
                        return;
                    }
                    applyPack(live, pack);
                    board = pack.storyboard();
                    // 缓存教案 JSON，下次命中动画时可恢复分镜
                    demos.putPackJson(conceptId, level, sceneSeed,
                            toPackJson(pack));
                }

                String html = generator.generate(board, level, sceneSeed);
                demos.put(conceptId, level, sceneSeed, html);
                live = sessions.get(sessionId);
                if (live == null || live.getGenerationToken() != token) {
                    return;
                }
                live.setDemoUrl("/api/demos/" + sessionId);
                live.setPhase("MOTTO_QUIZ");
                live.setError(null);
                fillMottoQuiz(live);
            } catch (Exception e) {
                MotifSession live = sessions.get(sessionId);
                if (live != null && live.getGenerationToken() == token) {
                    String msg = e.getMessage();
                    live.setError(msg == null || msg.isBlank() ? "动画生成失败" : msg);
                }
            }
        });
    }

    private void applyPack(MotifSession session, TeachingPackGenerator.Pack pack) {
        session.setStoryboard(pack.storyboard());
        session.setMotto(pack.motto());
        session.setQuiz(pack.quiz());
    }

    /** 把 Pack 再序列化成可缓存的 JSON（字段与 prompt 约定一致） */
    private static String toPackJson(TeachingPackGenerator.Pack pack) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"title\":").append(jsonStr(pack.storyboard().title()));
        sb.append(",\"motto\":").append(jsonStr(pack.motto()));
        sb.append(",\"beats\":[");
        List<Storyboard.Beat> beats = pack.storyboard().beats();
        for (int i = 0; i < beats.size(); i++) {
            Storyboard.Beat b = beats.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"who\":").append(jsonStr(b.who()))
                    .append(",\"action\":").append(jsonStr(b.action()))
                    .append(",\"result\":").append(jsonStr(b.result()))
                    .append(",\"principle\":").append(jsonStr(b.principle()))
                    .append('}');
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

    /** 仅在已进入 MOTTO_QUIZ（金牌同步路径）时填充口诀与题 */
    private void fillMottoQuizIfReady(MotifSession session) {
        if ("MOTTO_QUIZ".equals(session.getPhase()) || "DONE".equals(session.getPhase())) {
            fillMottoQuiz(session);
        }
    }

    /** 金牌概念用专属口诀题；非金牌优先保留教案包已写内容 */
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
            return;
        }
        // 非金牌：教案包已写入则保留
        if (session.getMotto() != null && session.getQuiz() != null && !session.getQuiz().isEmpty()) {
            return;
        }
        String title = session.getStoryboard() != null ? session.getStoryboard().title() : session.getConceptRaw();
        session.setMotto(title + "：看清「笨办法哪里别扭、聪明办法哪里省事」。");
        session.setQuiz(List.of(
            new QuizItem("q1", "这一课最该抓住的是什么？",
                List.of("概念的名字怎么读", "不用它时哪里别扭、用了哪里省事", "网页好不好看"), 1),
            new QuizItem("q2", "如果动画只喊概念名、没有对照，说明什么？",
                List.of("已经学懂了", "还没讲清原理，该换故事或更简单", "可以结束了"), 1)
        ));
    }

    /** 复制分镜并把第一拍 who 改名，用于体现 rewrite 生效 */
    private Storyboard renameFirstWho(Storyboard source, String newWho) {
        if (source == null || source.beats() == null || source.beats().isEmpty()) {
            return source;
        }
        List<Storyboard.Beat> beats = new ArrayList<>(source.beats());
        Storyboard.Beat first = beats.get(0);
        beats.set(0, new Storyboard.Beat(newWho, first.action(), first.result(), first.principle()));
        return new Storyboard(source.conceptId(), source.title(), List.copyOf(beats));
    }
}
