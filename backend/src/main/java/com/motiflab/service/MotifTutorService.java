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
import java.util.concurrent.ConcurrentHashMap;

/**
 * 母题授课会话状态机（v0.1，无 LLM）。
 * 关联：ConceptNormalizer、StoryboardService、DemoCache、MotifSession。
 */
public class MotifTutorService {

    private static final String LOOP_MOTTO = "循环就是同一件事，按次数做多遍。";

    private final ConceptNormalizer normalizer;
    private final StoryboardService storyboards;
    private final DemoCache demos;
    private final ConcurrentHashMap<String, MotifSession> sessions = new ConcurrentHashMap<>();

    public MotifTutorService(ConceptNormalizer normalizer, StoryboardService storyboards, DemoCache demos) {
        this.normalizer = normalizer;
        this.storyboards = storyboards;
        this.demos = demos;
    }

    /** 开始一课：建会话、取分镜、解析金牌 demo */
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
        fillLoopMottoQuizIfNeeded(session);
        sessions.put(id, session);
        return session;
    }

    /** 升简版等级并换 demo */
    public MotifSession simplify(String sessionId) {
        MotifSession session = get(sessionId);
        session.setLevel(normalizer.clampLevel(session.getLevel() + 1));
        applyDemoAndPhase(session);
        fillLoopMottoQuizIfNeeded(session);
        return session;
    }

    /** v0.1 重写：换 sceneSeed，轻微改分镜 who，再解析 demo */
    public MotifSession rewrite(String sessionId) {
        MotifSession session = get(sessionId);
        session.setSceneSeed(UUID.randomUUID().toString());
        session.setStoryboard(renameFirstWho(session.getStoryboard(), "小红"));
        applyDemoAndPhase(session);
        fillLoopMottoQuizIfNeeded(session);
        return session;
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

    /** 解析 demo：命中则挂 URL 并进入 MOTTO_QUIZ；未命中进入 DEMO */
    private void applyDemoAndPhase(MotifSession session) {
        Optional<Path> hit = demos.resolve(session.getConceptId(), session.getLevel());
        if (hit.isPresent()) {
            session.setDemoUrl("/api/demos/" + session.getId());
            session.setPhase("MOTTO_QUIZ");
        } else {
            session.setDemoUrl(null);
            session.setPhase("DEMO");
        }
    }

    /** loop 概念始终填充金牌口诀与题 */
    private void fillLoopMottoQuizIfNeeded(MotifSession session) {
        if (!"loop".equals(session.getConceptId())) {
            return;
        }
        session.setMotto(LOOP_MOTTO);
        session.setQuiz(List.of(
            new QuizItem("q1", "循环在做什么？",
                List.of("只做一次", "同样动作做多遍", "随便做"), 1),
            new QuizItem("q2", "上面叠积木叠了几次？",
                List.of("1", "3", "10"), 1)
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
