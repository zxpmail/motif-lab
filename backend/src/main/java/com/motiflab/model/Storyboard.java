package com.motiflab.model;

import java.util.List;

/** 母题分镜骨架。关联：StoryboardService、MotifTutorService。 */
public record Storyboard(
    String conceptId,
    String title,
    List<Beat> beats
) {
    public record Beat(String who, String action, String result, String principle) {}
}
