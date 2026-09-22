package org.rsinitsyn.quiz;

import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.List;
import java.util.UUID;

public interface QuizTestFixture {

    static QuestionModel.QuestionModelBuilder aQuestionModel() {
        return QuestionModel.builder()
                .id(UUID.randomUUID())
                .text("2 + 2 = ?")
                .type(QuestionType.TEXT)
                .categoryName("General")
                .answers(List.of(
                        QuestionModel.AnswerModel.builder()
                                .number(0)
                                .text("4")
                                .correct(true)
                                .build(),
                        QuestionModel.AnswerModel.builder()
                                .number(1)
                                .text("22")
                                .correct(false)
                                .build()
                ));
    }
}
