package org.rsinitsyn.quiz.model;

public enum AnswerHistory {
    ANSWERED_CORRECT,
    ANSWERED_WRONG,
    NOT_ANSWERED;

    public static AnswerHistory ofAnswerResult(Boolean answerResult) {
        if (answerResult == null) {
            return NOT_ANSWERED;
        }
        return answerResult ? ANSWERED_CORRECT : ANSWERED_WRONG;
    }
}
