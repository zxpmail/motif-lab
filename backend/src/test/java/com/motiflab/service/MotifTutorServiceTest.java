package com.motiflab.service;

import com.motiflab.dto.StartLessonRequest;
import com.motiflab.model.MotifSession;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** MotifTutorService 会话状态机测试。关联：DemoCache、StoryboardService。 */
class MotifTutorServiceTest {

    private MotifTutorService newTutor(Path dir) {
        ConceptNormalizer n = new ConceptNormalizer();
        return new MotifTutorService(n, new StoryboardService(n), new DemoCache(dir, "v1"));
    }

    @Test
    void start_loop_setsStoryboardAndCachedDemo() throws Exception {
        Path dir = Files.createTempDirectory("demo");
        MotifTutorService tutor = newTutor(dir);
        StartLessonRequest req = new StartLessonRequest();
        req.setConcept("循环");
        MotifSession s = tutor.start(req);
        assertEquals(0, s.getLevel());
        assertEquals("MOTTO_QUIZ", s.getPhase());
        assertNotNull(s.getStoryboard());
        assertNotNull(s.getDemoUrl());
        assertTrue(s.getDemoUrl().startsWith("/api/demos/"));
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
