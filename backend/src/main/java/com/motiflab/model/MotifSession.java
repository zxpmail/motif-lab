package com.motiflab.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 可变的母题授课会话状态。
 * 当前主路径：文字母题寓言 + 口诀检验；demoUrl 保留字段但默认不再生成动画。
 * 关联：MotifTutorService、QuizItem、Storyboard、ContrastRow。
 */
public class MotifSession {

    private String id;
    private String conceptRaw;
    private String conceptId;
    private int level;
    private String sceneSeed;
    private Storyboard storyboard;
    /** 已停用动画：恒为 null（字段保留兼容前端） */
    private String demoUrl;
    /** 母题寓言正文 */
    private String fable;
    /** 概念解释 2～3 句 */
    private String explanation;
    /** 隐喻对照表 */
    private List<ContrastRow> contrast;
    /** 母题提炼 X → Y */
    private String motif;
    private String motto;
    private List<QuizItem> quiz;
    private String phase;
    private String error;
    private Set<String> correctQuizIds = new HashSet<>();
    private long generationToken;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getConceptRaw() { return conceptRaw; }
    public void setConceptRaw(String conceptRaw) { this.conceptRaw = conceptRaw; }
    public String getConceptId() { return conceptId; }
    public void setConceptId(String conceptId) { this.conceptId = conceptId; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getSceneSeed() { return sceneSeed; }
    public void setSceneSeed(String sceneSeed) { this.sceneSeed = sceneSeed; }
    public Storyboard getStoryboard() { return storyboard; }
    public void setStoryboard(Storyboard storyboard) { this.storyboard = storyboard; }
    public String getDemoUrl() { return demoUrl; }
    public void setDemoUrl(String demoUrl) { this.demoUrl = demoUrl; }
    public String getFable() { return fable; }
    public void setFable(String fable) { this.fable = fable; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public List<ContrastRow> getContrast() { return contrast; }
    public void setContrast(List<ContrastRow> contrast) { this.contrast = contrast; }
    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
    public String getMotto() { return motto; }
    public void setMotto(String motto) { this.motto = motto; }
    public List<QuizItem> getQuiz() { return quiz; }
    public void setQuiz(List<QuizItem> quiz) { this.quiz = quiz; }
    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Set<String> getCorrectQuizIds() { return correctQuizIds; }
    public void setCorrectQuizIds(Set<String> correctQuizIds) { this.correctQuizIds = correctQuizIds; }
    public long getGenerationToken() { return generationToken; }
    public void setGenerationToken(long generationToken) { this.generationToken = generationToken; }
}
