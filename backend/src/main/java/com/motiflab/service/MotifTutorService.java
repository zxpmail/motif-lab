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
 * 金牌缓存命中同步进 MOTTO_QUIZ；未命中则异步 LLM 生成动画。
 * 关联：ConceptNormalizer、StoryboardService、DemoCache、AnimationGenerator、MotifSession。
 */
public class MotifTutorService {

    private static final String LOOP_MOTTO = "循环就是同一件事，按次数做多遍。";

    private final ConceptNormalizer normalizer;
    private final StoryboardService storyboards;
    private final DemoCache demos;
    /** 可空：单元测试或不启用生成时传 null */
    private final AnimationGenerator generator;
    private final ConcurrentHashMap<String, MotifSession> sessions = new ConcurrentHashMap<>();

    public MotifTutorService(ConceptNormalizer normalizer, StoryboardService storyboards, DemoCache demos) {
        this(normalizer, storyboards, demos, null);
    }

    public MotifTutorService(
            ConceptNormalizer normalizer,
            StoryboardService storyboards,
            DemoCache demos,
            AnimationGenerator generator) {
        this.normalizer = normalizer;
        this.storyboards = storyboards;
        this.demos = demos;
        this.generator = generator;
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
     * 解析 demo：金牌命中 → URL + MOTTO_QUIZ；未命中 → DEMO + 异步生成。
     */
    private void applyDemoAndPhase(MotifSession session) {
        session.setError(null);
        Optional<Path> hit = demos.resolve(session.getConceptId(), session.getLevel());
        if (hit.isPresent()) {
            session.setDemoUrl("/api/demos/" + session.getId());
            session.setPhase("MOTTO_QUIZ");
        } else {
            session.setDemoUrl(null);
            session.setPhase("DEMO");
            startAsyncGeneration(session);
        }
    }

    /** 缓存未命中时后台生成 HTML，写缓存并更新会话 */
    private void startAsyncGeneration(MotifSession session) {
        if (generator == null) {
            session.setError("动画生成器未就绪");
            return;
        }
        final String sessionId = session.getId();
        final String conceptId = session.getConceptId();
        final int level = session.getLevel();
        final Storyboard board = session.getStoryboard();
        final String sceneSeed = session.getSceneSeed();
        final long token = session.getGenerationToken();

        CompletableFuture.runAsync(() -> {
            try {
                String html = generator.generate(board, level, sceneSeed);
                demos.put(conceptId, level, html);
                MotifSession live = sessions.get(sessionId);
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

    /** 仅在已进入 MOTTO_QUIZ（金牌同步路径）时填充口诀与题 */
    private void fillMottoQuizIfReady(MotifSession session) {
        if ("MOTTO_QUIZ".equals(session.getPhase()) || "DONE".equals(session.getPhase())) {
            fillMottoQuiz(session);
        }
    }

    /** loop 用金牌口诀题；其它概念用通用口诀题 */
    private void fillMottoQuiz(MotifSession session) {
        if ("loop".equals(session.getConceptId())) {
            session.setMotto(LOOP_MOTTO);
            session.setQuiz(List.of(
                new QuizItem("q1", "循环在做什么？",
                    List.of("只做一次", "同样动作做多遍", "随便做"), 1),
                new QuizItem("q2", "上面叠积木叠了几次？",
                    List.of("1", "3", "10"), 1)
            ));
            return;
        }
        String title = session.getStoryboard() != null ? session.getStoryboard().title() : session.getConceptRaw();
        session.setMotto(title + "：先看懂故事，再记住这一下。");
        session.setQuiz(List.of(
            new QuizItem("q1", "刚才动画主要在讲什么？",
                List.of("随便玩玩", title, "和概念无关的东西"), 1),
            new QuizItem("q2", "学完这一课你最该记住什么？",
                List.of("口诀里那句话", "记住网页地址", "什么都不用记"), 0)
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
