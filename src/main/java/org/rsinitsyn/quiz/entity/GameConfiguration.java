package org.rsinitsyn.quiz.entity;

import lombok.Builder;

import java.time.Duration;

@Builder
public record GameConfiguration(
        boolean optionsEnabled,
        boolean hintsEnabled,
        boolean intrigueEnabled,
        Duration questionTime,
        Duration testTime
) {
}
