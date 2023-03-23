package org.rsinitsyn.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FourAnswersQuestionModel {
    private String id;
    @Length(min = 1, max = 300)
    private String text;
    @Length(min = 1, max = 100)
    private String correctAnswerText;
    @Length(min = 1, max = 100)
    private String secondOptionAnswerText;
    @Length(min = 1, max = 100)
    private String thirdOptionAnswerText;
    @Length(min = 1, max = 100)
    private String fourthOptionAnswerText;
}
