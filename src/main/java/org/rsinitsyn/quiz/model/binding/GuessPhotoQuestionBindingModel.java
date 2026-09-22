package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class GuessPhotoQuestionBindingModel extends AbstractQuestionBindingModel {

    private final Map<String, InputStream> photoFilenameToResource = new LinkedHashMap<>();
    private List<String> photoFilenames = new ArrayList<>();
    @NotBlank
    @Setter
    private String answerText;

    public GuessPhotoQuestionBindingModel(final String id, final String text, final String answerDescriptionText,
                                          final String photoLocation, final String category, final String hintsText,
                                          final List<String> photoFilenames, final String answerText) {
        super(id, text, answerDescriptionText, photoLocation, category, hintsText);
        this.photoFilenames = photoFilenames;
        this.answerText = answerText;
    }

    public void addPhotoStream(String filename, InputStream resource) {
        photoFilenameToResource.put(filename, resource);
    }

    public void removePhotoStream(String filename) {
        photoFilenameToResource.remove(filename);
    }

    public void cleanPhotos() {
        photoFilenameToResource.clear();
    }
}
