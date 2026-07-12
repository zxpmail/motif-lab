package com.motiflab.dto;

/**
 * 检验题答题请求体。
 * 关联：LessonController、MotifTutorService。
 */
public class AnswerRequest {

    private String quizId;
    private int choiceIndex;

    public AnswerRequest() {}

    public AnswerRequest(String quizId, int choiceIndex) {
        this.quizId = quizId;
        this.choiceIndex = choiceIndex;
    }

    public String getQuizId() {
        return quizId;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public int getChoiceIndex() {
        return choiceIndex;
    }

    public void setChoiceIndex(int choiceIndex) {
        this.choiceIndex = choiceIndex;
    }
}
