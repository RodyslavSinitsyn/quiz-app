package org.rsinitsyn.quiz.model;

import lombok.Builder;
import lombok.Getter;

import static org.rsinitsyn.quiz.model.HintsState.disabledHintsState;

@Builder
@Getter
public class AnswerLayoutRequest {
    private QuestionModel question;
    @Builder.Default
    private HintsState hintsState = disabledHintsState();
}
