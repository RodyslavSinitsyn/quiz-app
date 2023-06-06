package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * Data model for Create/Edit/Delete precision question.
 */
@Data
@NoArgsConstructor
public class PrecisionQuestionBindingModel extends AbstractQuestionBindingModel {
    @NotNull
    private Double answerText;
    @NotNull
    private Double range;
}
