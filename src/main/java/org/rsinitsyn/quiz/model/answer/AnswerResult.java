package org.rsinitsyn.quiz.model.answer;

import org.rsinitsyn.quiz.entity.AnswerStatus;

import static org.rsinitsyn.quiz.entity.AnswerStatus.answerStatus;

public record AnswerResult(AnswerStatus status, int maxCount, int correctCount) {

    public static AnswerResult oneOptionResult(boolean correct) {
        return new AnswerResult(answerStatus(correct), 1, correct ? 1 : 0);
    }
}
