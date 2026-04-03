package org.rsinitsyn.quiz.model.cleverest;

import org.rsinitsyn.quiz.model.QuestionModel;

public record UserRefreshState(QuestionModel question,
                               int questionNumber,
                               int totalQuestionsSize,
                               boolean answerGiven) {
}
