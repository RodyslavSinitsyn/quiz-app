package org.rsinitsyn.quiz.model;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.utils.QuizUtils;

// TODO Need entities ID here
@Data
@NoArgsConstructor
public class QuizQuestionModel {
    private String text;
    private QuestionType type;
    private String photoFilename;
    private String categoryName;
    private Set<QuizAnswerModel> answers = new HashSet<>();

    private InputStream photoInputStream;

    public QuizQuestionModel(String text, QuestionType type, String photoFilename, String categoryName, Set<QuizAnswerModel> answers) {
        this.text = text;
        this.type = type;
        this.photoFilename = photoFilename;
        this.categoryName = categoryName;
        this.answers = answers;
    }

    @SneakyThrows
    public InputStream openStream() {
        closePhotoStream();
        photoInputStream = new FileInputStream(QuizUtils.getImageFile(photoFilename));
        return photoInputStream;
    }

    @SneakyThrows
    public void closePhotoStream() {
        if (photoInputStream != null) {
            System.out.println("Closing stream...");
            photoInputStream.close();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuizAnswerModel {
        private String text;
        private boolean correct;
    }
}
