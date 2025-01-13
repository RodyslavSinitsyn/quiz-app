package org.rsinitsyn.quiz.model;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Builder
@Getter
public class QuestionLayoutRequest {
   private QuestionModel question;
   private boolean isAdmin = false;
   private String imageHeight = "25em";
   private List<String> textClasses = Collections.emptyList();
   private HintsState hintsState = HintsState.disabled();
}
