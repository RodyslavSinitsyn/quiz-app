package org.rsinitsyn.quiz.component.custom.question;

import org.rsinitsyn.quiz.model.QuestionLayoutRequest;

public class QuestionLayoutFactory {

    public static BaseQuestionLayout createQuestionLayout(QuestionLayoutRequest request) {
        return new BaseQuestionLayout(request);
    }
}
