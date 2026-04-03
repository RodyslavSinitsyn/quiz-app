package org.rsinitsyn.quiz.entity;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum AnswerStatus {
    UNKNOWN(null),
    CORRECT(true),
    WRONG(false),
    PARTIAL(true);

    public final Boolean boolVal;

    public boolean correct() {
        return boolVal != null && boolVal;
    }

    public static AnswerStatus answerStatus(Boolean correct) {
        if (correct == null) {
            return UNKNOWN;
        }
        return correct ? CORRECT : WRONG;
    }

    public static AnswerStatus answerStatus(int count, int maxCount) {
        if (count < 0 || maxCount <= 0) {
            throw new IllegalArgumentException("count can't be 0 and clickLimit must be positive");
        }
        if (count == 0) {
            return WRONG;
        }
        return count == maxCount ? CORRECT : PARTIAL;
    }
}
