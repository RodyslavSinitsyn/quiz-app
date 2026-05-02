package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.rsinitsyn.quiz.validator.PhotoUrlValid;

import java.io.InputStream;

@NoArgsConstructor
@Data
public abstract class AbstractQuestionBindingModel {
    public static final int TEXT_LENGTH_LIMIT = 1000;

    private String id;
    @Length(min = 1, max = TEXT_LENGTH_LIMIT)
    @NotBlank
    private String text;
    private String answerDescriptionText;
    @Length(max = 1000)
    @PhotoUrlValid
    private String photoLocation;
    private String category;
    private String hintsText;
    private InputStream audio;

    public AbstractQuestionBindingModel(final String id,
                                        final String text,
                                        final String answerDescriptionText,
                                        final String photoLocation,
                                        final String category,
                                        final String hintsText) {
        this.id = id;
        this.text = text;
        this.answerDescriptionText = answerDescriptionText;
        this.photoLocation = photoLocation;
        this.category = category;
        this.hintsText = hintsText;
    }
}
