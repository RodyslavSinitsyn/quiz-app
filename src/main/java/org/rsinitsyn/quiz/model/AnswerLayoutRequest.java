package org.rsinitsyn.quiz.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AnswerLayoutRequest {
    private QuestionModel question;
    private HintsState hintsState;
}
