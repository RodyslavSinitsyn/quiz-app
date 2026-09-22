package org.rsinitsyn.quiz.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AnswerStatus {
    UNKNOWN,
    CORRECT,
    WRONG,
    PARTIAL
    ;

    public boolean correct() {
        return this == CORRECT || this == PARTIAL;
    }

    public boolean wrong() {
        return this == WRONG;
    }

    public boolean answered() {
        return this != UNKNOWN;
    }

    public static AnswerStatus answerStatus(Boolean correct) {
        if (correct == null) {
            return UNKNOWN;
        }
        return correct ? CORRECT : WRONG;
    }

    public static AnswerStatus answerStatus(int correctCount, int maxCount) {
        if (correctCount < 0 || maxCount <= 0) {
            throw new IllegalArgumentException("count can't be 0 and maxCount must be positive");
        }
        if (correctCount == 0) {
            return WRONG;
        }
        return correctCount == maxCount ? CORRECT : PARTIAL;
    }
}
