package org.rsinitsyn.quiz.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

import static org.rsinitsyn.quiz.model.HintsState.disabledHintsState;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class QuestionLayoutRequest {
   private QuestionModel question;
   private boolean host = false;
   private String imageHeight = "25em";
   private HintsState hintsState = disabledHintsState();
   private boolean renderCategory = true;
}
