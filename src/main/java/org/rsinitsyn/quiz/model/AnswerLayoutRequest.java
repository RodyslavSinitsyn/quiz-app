package org.rsinitsyn.quiz.model;

import lombok.Builder;
import lombok.Getter;
import org.rsinitsyn.quiz.model.answer.AnswerBet;

import java.util.Optional;

import static org.rsinitsyn.quiz.model.HintsState.disabledHintsState;

@Builder
@Getter
public class AnswerLayoutRequest {
    private QuestionModel question;
    @Builder.Default
    private Optional<String> username = Optional.empty();
    @Builder.Default
    private HintsState hintsState = disabledHintsState();
    @Builder.Default
    private Optional<AnswerBet> answerBet = Optional.empty();
}
