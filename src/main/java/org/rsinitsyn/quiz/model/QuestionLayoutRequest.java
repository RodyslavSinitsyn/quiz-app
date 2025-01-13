package org.rsinitsyn.quiz.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Accessors(chain = true, fluent = true)
public class QuestionLayoutRequest {
   private QuestionModel question;
   private boolean isAdmin = false;
   private String imageHeight = "25em";
   private List<String> textClasses = Collections.emptyList();
   private HintsState hintsState = HintsState.disabled();
}
