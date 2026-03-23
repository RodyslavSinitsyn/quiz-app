package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TopQuestionBindingModel extends AbstractQuestionBindingModel {
    @NotBlank
    private String topListText;
    private boolean sequence;

    public TopQuestionBindingModel(String id,
                                   String text,
                                   String topListText,
                                   boolean sequence,
                                   String originalPhotoUrl,
                                   String category,
                                   String answerDescriptionText) {
        super(id, text, answerDescriptionText, originalPhotoUrl, category);
        this.topListText = topListText;
        this.sequence = sequence;
    }
}
