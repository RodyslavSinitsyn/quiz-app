package org.rsinitsyn.quiz.component.custom.question;

import org.rsinitsyn.quiz.model.QuestionLayoutRequest;

public class QuestionLayoutFactory {

    public static BaseQuestionLayout get(QuestionLayoutRequest request) {
        var type = request.question().getType();
        return switch (type) {
            case PRECISION -> new PrecisionBaseQuestionLayout(request);
            default -> new BaseQuestionLayout(request);
        };
    }
}
