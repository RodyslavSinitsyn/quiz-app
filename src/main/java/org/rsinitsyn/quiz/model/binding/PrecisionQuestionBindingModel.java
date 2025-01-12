package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    public PrecisionQuestionBindingModel(String id,
                                         String text,
                                         String answerDescriptionText,
                                         String photoLocation,
                                         String category,
                                         Double answerText,
                                         Double range) {
        super(id, text, answerDescriptionText, photoLocation, category);
        this.answerText = answerText;
        this.range = range;
    }
}
