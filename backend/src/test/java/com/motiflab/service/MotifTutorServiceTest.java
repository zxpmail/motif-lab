package com.motiflab.service;

import com.motiflab.dto.StartLessonRequest;
import com.motiflab.model.MotifSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** MotifTutorService：金牌文字课；无动画。关联：DemoCache、StoryboardService。 */
class MotifTutorServiceTest {

    private MotifTutorService newTutor(Path dir) {
        ConceptNormalizer n = new ConceptNormalizer();
        return new MotifTutorService(n, new StoryboardService(n), new DemoCache(dir, "v3"));
    }

    @Test
    void start_loop_setsFableAndMotto_noAnimation() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("循环");
        MotifSession s = tutor.start(req);
        assertEquals(0, s.getLevel());
        assertEquals("MOTTO_QUIZ", s.getPhase());
        assertNotNull(s.getStoryboard());
        assertNull(s.getDemoUrl());
        assertNotNull(s.getFable());
        assertFalse(s.getFable().isBlank());
        assertEquals(2, s.getQuiz().size());
    }

    @Test
    void simplify_bumpsLevel() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("循环");
        MotifSession s = tutor.start(req);
        MotifSession updated = tutor.simplify(s.getId());
        assertEquals(1, updated.getLevel());
        assertEquals(1, tutor.get(s.getId()).getLevel());
    }

    @Test
    void simplify_atL2_staysL2() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("循环");
        MotifSession s = tutor.start(req);
        tutor.simplify(s.getId());
        tutor.simplify(s.getId());
        tutor.simplify(s.getId());
        assertEquals(2, tutor.get(s.getId()).getLevel());
    }

    @Test
    void answer_correct_eventually_done() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("循环");
        MotifSession s = tutor.start(req);
        assertTrue(tutor.answer(s.getId(), "q1", 1));
        assertNotEquals("DONE", tutor.get(s.getId()).getPhase());
        assertTrue(tutor.answer(s.getId(), "q2", 1));
        assertEquals("DONE", tutor.get(s.getId()).getPhase());
    }

    @Test
    void start_variable_goldText() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("变量");
        MotifSession s = tutor.start(req);
        assertEquals("variable", s.getConceptId());
        assertEquals("MOTTO_QUIZ", s.getPhase());
        assertNull(s.getDemoUrl());
        assertTrue(s.getMotto().contains("盒子"));
        assertNotNull(s.getFable());
    }

    @Test
    void start_function_goldText() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("函数");
        MotifSession s = tutor.start(req);
        assertEquals("function", s.getConceptId());
        assertEquals("MOTTO_QUIZ", s.getPhase());
        assertNull(s.getDemoUrl());
        assertTrue(s.getMotto().contains("机器"));
    }

    @Test
    void simplify_afterDone_clearsQuizProgress() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("循环");
        MotifSession s = tutor.start(req);
        tutor.answer(s.getId(), "q1", 1);
        tutor.answer(s.getId(), "q2", 1);
        assertEquals("DONE", tutor.get(s.getId()).getPhase());
        MotifSession again = tutor.simplify(s.getId());
        assertTrue(again.getCorrectQuizIds().isEmpty());
        assertEquals("MOTTO_QUIZ", again.getPhase());
        assertEquals(1, again.getLevel());
    }
}
