package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.rsinitsyn.quiz.validator.PhotoUrlValid;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class AbstractQuestionBindingModel {
    public static final int TEXT_LENGTH_LIMIT = 1000;

    private String id;
    @Length(min = 1, max = TEXT_LENGTH_LIMIT)
    @NotBlank
    private String text;
    private String answerDescriptionText;
    @Length(max = 1000)
    @PhotoUrlValid
    private String photoLocation;
}
