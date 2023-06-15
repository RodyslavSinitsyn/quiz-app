package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkQuestionBindingModel extends AbstractQuestionBindingModel {
    @NotBlank
    private String leftAnswers;
    @NotBlank
    private String rightAnswers;

    public LinkQuestionBindingModel(String id,
                                    String text,
                                    String answerDescriptionText,
                                    String photoLocation,
                                    String leftAnswers,
                                    String rightAnswers) {
        super(id, text, answerDescriptionText, photoLocation);
        this.leftAnswers = leftAnswers;
        this.rightAnswers = rightAnswers;
    }
}
