package org.rsinitsyn.quiz.model;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class QuestionCategoryBindingModel {
    @Length(min = 1, max = 30)
    private String categoryName;
}
