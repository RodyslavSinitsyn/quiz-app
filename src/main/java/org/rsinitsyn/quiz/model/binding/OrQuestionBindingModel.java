package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrQuestionBindingModel extends AbstractQuestionBindingModel {
    @NotBlank
    private String correctAnswerText;
    @NotBlank
    private String optionAnswerText;

    public OrQuestionBindingModel(
            String id,
            String text,
            String correctAnswerText,
            String optionAnswerText,
            String photoLocation,
            String category,
            String answerDescriptionText) {
        super(id, text, answerDescriptionText, photoLocation, category);
        this.correctAnswerText = correctAnswerText;
        this.optionAnswerText = optionAnswerText;
    }
}
