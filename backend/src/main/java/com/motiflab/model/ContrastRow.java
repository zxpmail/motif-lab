package com.motiflab.model;

/** 母题对照表一行：故事元素 ↔ 概念部分。关联：MotifSession、TeachingPackGenerator。 */
public record ContrastRow(String story, String concept) {}
