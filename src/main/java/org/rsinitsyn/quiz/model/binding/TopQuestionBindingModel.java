package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TopQuestionBindingModel extends AbstractQuestionBindingModel {
    private String id;
    @NotBlank
    private String topListText;

    public TopQuestionBindingModel(String id, String text, String topListText, String originalPhotoUrl, String answerDescriptionText) {
        super(id, text, answerDescriptionText, originalPhotoUrl);
        this.topListText = topListText;
    }
}
