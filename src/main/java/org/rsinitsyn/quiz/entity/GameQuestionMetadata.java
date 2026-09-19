package org.rsinitsyn.quiz.entity;

import lombok.Builder;

import java.util.UUID;

@Builder
public record GameQuestionMetadata(
        int order,
        Integer round,
        UUID categoryId,
        Double multiplier,
        Integer difficulty
) {
}
