package com.motiflab.model;

import java.util.List;

/** 检验题条目。关联：MotifSession、MotifTutorService。 */
public record QuizItem(
    String id,
    String question,
    List<String> choices,
    int answerIndex
) {}
