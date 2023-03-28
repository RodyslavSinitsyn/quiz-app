package org.rsinitsyn.quiz.model;

import jakarta.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.rsinitsyn.quiz.validator.PhotoUrlValid;

/**
 * Data model for Create/Edit/Delete quiz question.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FourAnswersQuestionBindingModel {
    public static final int TEXT_LENGTH_LIMIT = 1000;
    private String id;
    @NotBlank
    @Length(min = 1, max = TEXT_LENGTH_LIMIT)
    private String text;
    private String category;
    private String author;
    @NotBlank
    @Length(min = 1, max = 100)
    private String correctAnswerText;
    @NotBlank
    @Length(min = 1, max = 100)
    private String secondOptionAnswerText;
    @NotBlank
    @Length(min = 1, max = 100)
    private String thirdOptionAnswerText;
    @NotBlank
    @Length(min = 1, max = 255)
    private String fourthOptionAnswerText;
    @Length(max = 1000)
    @PhotoUrlValid
    private String photoLocation;

    public boolean optionsRepeated() {
        Set<String> options = new HashSet<>();
        options.add(correctAnswerText);
        options.add(secondOptionAnswerText);
        options.add(thirdOptionAnswerText);
        options.add(fourthOptionAnswerText);
        return options.size() != 4;
    }
}
