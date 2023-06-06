package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.rsinitsyn.quiz.validator.PhotoUrlValid;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PhotoQuestionBindingModel extends AbstractQuestionBindingModel {
    @PhotoUrlValid
    @NotBlank
    private String correctOption;
    @PhotoUrlValid
    @NotBlank
    private String optionTwo;
    @PhotoUrlValid
    @NotBlank
    private String optionThree;
    @PhotoUrlValid
    @NotBlank
    private String optionFour;

    public PhotoQuestionBindingModel(String id,
                                     String text,
                                     String answerDescriptionText,
                                     String photoLocation,
                                     String correctOption,
                                     String optionTwo,
                                     String optionThree,
                                     String optionFour) {
        super(id, text, answerDescriptionText, photoLocation);
        this.correctOption = correctOption;
        this.optionTwo = optionTwo;
        this.optionThree = optionThree;
        this.optionFour = optionFour;
    }
}
