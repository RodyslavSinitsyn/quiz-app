package org.rsinitsyn.quiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionType;

@Getter
@Setter
@EqualsAndHashCode(exclude = {"playersAnswersHistory", "answers"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionModel {
    private UUID id;
    private String text;
    private QuestionType type;
    private String photoFilename;
    private String audioFilename;
    private String categoryName;
    private boolean optionsOnly;
    private Integer validRange;
    private Map<String, AnswerHistory> playersAnswersHistory;
    private Set<AnswerModel> answers;

    // for cleverest
    private boolean alreadyAnswered;
    private int points;

    public AnswerModel getFirstCorrectAnswer() {
        return answers.stream()
                .filter(AnswerModel::isCorrect)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No correct answer!"));
    }

    public List<AnswerModel> getShuffledAnswers() {
        List<AnswerModel> answerList = new ArrayList<>(answers);
        Collections.shuffle(answerList);
        return answerList;
    }

    public boolean areAnswersCorrect(Set<AnswerModel> userAnswers) {
        if (type.equals(QuestionType.TEXT)) {
            return userAnswers.stream().anyMatch(AnswerModel::isCorrect);
        } else if (type.equals(QuestionType.MULTI)) {
            long correctAnswersCount = this.answers.stream().filter(AnswerModel::isCorrect).count();
            long userCorrectAnswersCount = userAnswers.stream().filter(AnswerModel::isCorrect).count();
            boolean userHasOnlyCorrectAnswers = userCorrectAnswersCount == userAnswers.size();
            return userHasOnlyCorrectAnswers && correctAnswersCount == userCorrectAnswersCount;
        } else if (type.equals(QuestionType.PRECISION)) {
            AnswerModel answerModel = userAnswers.stream().findFirst().orElseThrow(() -> new RuntimeException("No answer"));
            if (!StringUtils.isNumeric(answerModel.getText())) {
                return Boolean.FALSE;
            }
            int userAnswer = Integer.parseInt(answerModel.getText());
            int validAnswer = Integer.parseInt(answers.stream().findFirst().orElseThrow().getText());
            return Math.abs(validAnswer - userAnswer) <= validRange;
        } else if (type.equals(QuestionType.TOP)) {
            return false;
        } else {
            throw new IllegalStateException("QuestionType not defined");
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerModel {
        private String text;
        private boolean correct;

        public static AnswerModel defaultWrong() {
            return new AnswerModel(
                    "Неверный",
                    false
            );
        }
    }
}
