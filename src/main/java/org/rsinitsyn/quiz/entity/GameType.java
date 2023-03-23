package org.rsinitsyn.quiz.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GameType {
    QUIZ(true),
    SI_GAME(false);
    private final boolean enabled;
}
