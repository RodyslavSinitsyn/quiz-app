package org.rsinitsyn.quiz.model.binding;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LinkQuestionBindingModel extends AbstractQuestionBindingModel {
    private String leftAnswers;
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
