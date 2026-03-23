package org.rsinitsyn.quiz.component.custom.question;

import org.rsinitsyn.quiz.model.QuestionLayoutRequest;

public class QuestionLayoutFactory {

    public static BaseQuestionLayout createQuestionLayout(QuestionLayoutRequest request) {
        var type = request.question().getType();
        return new BaseQuestionLayout(request);
    }
}
