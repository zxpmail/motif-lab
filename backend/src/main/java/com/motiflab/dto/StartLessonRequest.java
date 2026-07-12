package com.motiflab.dto;

/** 开始一课的请求体。关联：MotifTutorService。 */
public class StartLessonRequest {

    private String concept;

    public StartLessonRequest() {}

    public StartLessonRequest(String concept) {
        this.concept = concept;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }
}
