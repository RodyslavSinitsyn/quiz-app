package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class QuestionCategoryBindingModel  extends AbstractQuestionBindingModel {
    @NotBlank
    @Length(min = 1, max = 30)
    private String categoryName;
}
