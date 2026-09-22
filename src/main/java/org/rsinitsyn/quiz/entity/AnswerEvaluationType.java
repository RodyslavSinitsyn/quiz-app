package org.rsinitsyn.quiz.entity;

public enum AnswerEvaluationType {
    AUTOMATIC,
    MANUAL
    ;

    public static AnswerEvaluationType ofManuallyApproved(boolean manuallyApproved) {
        return manuallyApproved ? MANUAL : AUTOMATIC;
    }
}