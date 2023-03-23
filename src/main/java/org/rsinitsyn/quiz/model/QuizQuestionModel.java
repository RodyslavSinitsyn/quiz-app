package org.rsinitsyn.quiz.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionModel implements Serializable {
    private String text;
    private Set<QuizAnswerModel> answers = new HashSet<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizAnswerModel implements Serializable {
        private String text;
        private boolean correct;
    }
}
