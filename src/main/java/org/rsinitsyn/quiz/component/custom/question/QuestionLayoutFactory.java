package org.rsinitsyn.quiz.component.custom.question;

import org.rsinitsyn.quiz.model.QuestionModel;

public class QuestionLayoutFactory {

    public BaseQuestionLayout get(QuestionModel question) {
        var type = question.getType();
        return switch (type) {
            case PRECISION -> new PrecisionBaseQuestionLayout(question);
            default -> new BaseQuestionLayout(question);
        };
    }
}
