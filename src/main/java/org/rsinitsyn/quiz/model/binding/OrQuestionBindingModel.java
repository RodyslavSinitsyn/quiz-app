package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrQuestionBindingModel {
    private String id;
    @NotBlank
    private String text;
    @NotBlank
    private String correctAnswerText;
    @NotBlank
    private String optionAnswerText;
}
