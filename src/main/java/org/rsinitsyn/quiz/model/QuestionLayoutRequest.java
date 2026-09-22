package org.rsinitsyn.quiz.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.rsinitsyn.quiz.model.answer.AnswerBet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.rsinitsyn.quiz.model.HintsState.disabledHintsState;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class QuestionLayoutRequest {
   private QuestionModel question;
   private boolean host = false;
   private Optional<String> username = Optional.empty();
   private String imageHeight = "25em";
   private HintsState hintsState = disabledHintsState();
   private Optional<AnswerBet> answerBet = Optional.empty();
   private boolean renderCategory = true;
   private boolean manualAnswer = false;
   private boolean hideAnswers = false;
}
