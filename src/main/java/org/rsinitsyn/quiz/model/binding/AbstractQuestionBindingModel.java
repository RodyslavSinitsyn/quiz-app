package org.rsinitsyn.quiz.model.binding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.rsinitsyn.quiz.validator.PhotoUrlValid;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AbstractQuestionBindingModel {
    private String answerDescriptionText;
    @Length(max = 1000)
    @PhotoUrlValid
    private String photoLocation;
}
