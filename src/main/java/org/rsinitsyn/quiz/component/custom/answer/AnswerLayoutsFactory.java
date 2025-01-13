package org.rsinitsyn.quiz.component.custom.answer;

import org.rsinitsyn.quiz.model.AnswerLayoutRequest;

public class AnswerLayoutsFactory {

    public static AbstractAnswersLayout get(AnswerLayoutRequest request) {
        var questionType = request.getQuestion().getType();
        return switch (questionType) {
            case TEXT -> new AnswersLayout(request);
            case LINK -> new LinkAnswersLayout(request);
            case MULTI -> new MultiAnswersLayout(request);
            case OR -> new OrAnswersLayout(request);
            case PHOTO -> new PhotoAnswersLayout(request);
            case PRECISION -> new PrecisionAnswersLayout(request);
            case TOP -> new TopAnswersLayout(request);
            default -> throw new IllegalArgumentException("Unknown question type: " + questionType);
        };
    }
}
