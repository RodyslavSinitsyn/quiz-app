package org.rsinitsyn.quiz.entity;

import java.util.UUID;

public record GameQuestionMetadata(
        int order,
        Integer round,
        UUID categoryId,
        Double multiplier,
        Integer difficulty
) {
}
