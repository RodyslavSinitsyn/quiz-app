package org.rsinitsyn.quiz.model;

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
public class PrecisionQuestionBindingModel {
    public static final int TEXT_LENGTH_LIMIT = 1000;
    private String id;
    @NotBlank
    @Length(min = 1, max = TEXT_LENGTH_LIMIT)
    private String text;
    @NotNull
    private Double answerText;
    @NotNull
    private Double range;
}
