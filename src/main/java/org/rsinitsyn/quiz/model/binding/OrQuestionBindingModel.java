package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
public class OrQuestionBindingModel extends AbstractQuestionBindingModel {
    private String id;
    @NotBlank
    private String text;
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
            String answerDescriptionText) {
        super(answerDescriptionText, photoLocation);
        this.id = id;
        this.text = text;
        this.correctAnswerText = correctAnswerText;
        this.optionAnswerText = optionAnswerText;
    }
}
