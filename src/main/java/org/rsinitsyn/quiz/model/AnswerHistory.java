package org.rsinitsyn.quiz.model;

public enum AnswerHistory {
    ANSWERED_CORRECT,
    ANSWERED_WRONG,
    NOT_ANSWERED;

    public static AnswerHistory ofAnswerResult(Boolean status) {
        if (status == null) {
            return NOT_ANSWERED;
        }
        return status ? ANSWERED_CORRECT : ANSWERED_WRONG;
    }
}
