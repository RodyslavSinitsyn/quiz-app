package org.rsinitsyn.quiz.component.custom.answer;

import org.rsinitsyn.quiz.model.QuestionModel;

public class AnswerLayoutsFactory {

    public static AbstractAnswersLayout get(QuestionModel questionModel) {
        var questionType = questionModel.getType();
        return switch (questionType) {
            case TEXT -> new AnswersLayout(questionModel);
            case LINK -> new LinkAnswersLayout(questionModel);
            case MULTI -> new MultiAnswersLayout(questionModel);
            case OR -> new AnswersLayout(questionModel);
            case PHOTO -> new PhotoAnswersLayout(questionModel);
            case TOP -> new TopAnswersLayout(questionModel);
            default -> throw new IllegalArgumentException("Unknown question type: " + questionType);
        };
    }
}
