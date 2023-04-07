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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"photoInputStream", "playersAnswersHistory", "answers"})
@Builder
public class QuizQuestionModel {
    private UUID id;
    private String text;
    private QuestionType type;
    private String photoFilename;
    private String audioFilename;
    private String categoryName;
    private boolean optionsOnly;
    private Integer validRange;
    private Map<String, AnswerHistory> playersAnswersHistory;
    private Set<QuizAnswerModel> answers;

    // for cleverest
    private boolean special = false;

    @Setter(AccessLevel.NONE)
    private InputStream photoInputStream;

    public QuizAnswerModel getFirstCorrectAnswer() {
        return answers.stream()
                .filter(QuizAnswerModel::isCorrect)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No correct answer!"));
    }

    public List<QuizAnswerModel> getShuffledAnswers() {
        List<QuizQuestionModel.QuizAnswerModel> answerList = new ArrayList<>(answers);
        Collections.shuffle(answerList);
        return answerList;
    }

    public boolean areAnswersCorrect(Set<QuizAnswerModel> userAnswers) {
        if (type.equals(QuestionType.TEXT)) {
            return userAnswers.stream().anyMatch(QuizAnswerModel::isCorrect);
        } else if (type.equals(QuestionType.MULTI)) {
            long correctAnswersCount = this.answers.stream().filter(QuizAnswerModel::isCorrect).count();
            long userCorrectAnswersCount = userAnswers.stream().filter(QuizAnswerModel::isCorrect).count();
            boolean userHasOnlyCorrectAnswers = userCorrectAnswersCount == userAnswers.size();
            return userHasOnlyCorrectAnswers && correctAnswersCount == userCorrectAnswersCount;
        } else if (type.equals(QuestionType.PRECISION)) {
            QuizAnswerModel answerModel = userAnswers.stream().findFirst().orElseThrow(() -> new RuntimeException("No answer"));
            int userAnswer = Integer.parseInt(answerModel.getText());
            int validAnswer = Integer.parseInt(answers.stream().findFirst().orElseThrow().getText());
            return Math.abs(validAnswer - userAnswer) <= validRange;
        } else if (type.equals(QuestionType.TOP)) {
            return false;
        } else {
            throw new IllegalStateException("QuestionType not defined");
        }
    }

    @SneakyThrows
    public InputStream openStream() {
        closePhotoStream();
        photoInputStream = new FileInputStream(QuizUtils.readImageFile(photoFilename));
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
