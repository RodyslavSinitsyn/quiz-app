package org.rsinitsyn.quiz.model;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Data
@Builder
public class QuizQuestionModel {
    private UUID id;
    private String text;
    private QuestionType type;
    private String photoFilename;
    private String categoryName;
    private Map<String, AnswerHistory> playersAnswersHistory;
    private Set<QuizAnswerModel> answers;
    private boolean optionsEnabled = true;

    @Setter(AccessLevel.NONE)
    private InputStream photoInputStream;

    public List<QuizAnswerModel> getShuffledAnswers() {
        List<QuizQuestionModel.QuizAnswerModel> answerList = new ArrayList<>(answers);
        Collections.shuffle(answerList);
        return answerList;
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
