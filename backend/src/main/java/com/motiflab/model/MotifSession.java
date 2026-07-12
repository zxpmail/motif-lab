package com.motiflab.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可变的母题授课会话状态。
 * 关联：MotifTutorService、QuizItem、Storyboard。
 */
public class MotifSession {

    private String id;
    private String conceptRaw;
    private String conceptId;
    private int level;
    private String sceneSeed;
    private Storyboard storyboard;
    private String demoUrl;
    private String motto;
    private List<QuizItem> quiz;
    private String phase;
    /** 异步生成失败时的可读错误；成功或金牌命中时为 null */
    private String error;
    /** 已答对的题目 id，用于判定 DONE */
    private Set<String> correctQuizIds = new HashSet<>();
    /** 异步生成世代号；simplify/rewrite 时递增，过期任务不得写回 */
    private long generationToken;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getConceptRaw() {
        return conceptRaw;
    }

    public void setConceptRaw(String conceptRaw) {
        this.conceptRaw = conceptRaw;
    }

    public String getConceptId() {
        return conceptId;
    }

    public void setConceptId(String conceptId) {
        this.conceptId = conceptId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getSceneSeed() {
        return sceneSeed;
    }

    public void setSceneSeed(String sceneSeed) {
        this.sceneSeed = sceneSeed;
    }

    public Storyboard getStoryboard() {
        return storyboard;
    }

    public void setStoryboard(Storyboard storyboard) {
        this.storyboard = storyboard;
    }

    public String getDemoUrl() {
        return demoUrl;
    }

    public void setDemoUrl(String demoUrl) {
        this.demoUrl = demoUrl;
    }

    public String getMotto() {
        return motto;
    }

    public void setMotto(String motto) {
        this.motto = motto;
    }

    public List<QuizItem> getQuiz() {
        return quiz;
    }

    public void setQuiz(List<QuizItem> quiz) {
        this.quiz = quiz;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Set<String> getCorrectQuizIds() {
        return correctQuizIds;
    }

    public void setCorrectQuizIds(Set<String> correctQuizIds) {
        this.correctQuizIds = correctQuizIds;
    }

    public long getGenerationToken() {
        return generationToken;
    }

    public void setGenerationToken(long generationToken) {
        this.generationToken = generationToken;
    }
}
