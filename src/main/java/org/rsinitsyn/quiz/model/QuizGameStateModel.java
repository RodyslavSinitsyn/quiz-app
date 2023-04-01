package org.rsinitsyn.quiz.model;

import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.rsinitsyn.quiz.entity.GameStatus;

/**
 * Represents the state of one specific quiz game
 */
// TODO Split to *SettingsBindingModel and *StateModel
@Getter
@Setter
public class QuizGameStateModel {
    @NotBlank
    @Length(min = 1, max = 30)
    private String gameName = "Test";
    @NotBlank
    @Length(min = 1, max = 30)
    private String playerName = "Test";
    private boolean answerOptionsEnabled;
    private boolean timerEnabled;
    private boolean hintsEnabled;
    private boolean intrigueEnabled;
    private Set<QuizQuestionModel> questions = new HashSet<>();

    // state props
    private UUID gameId;
    @Setter(AccessLevel.NONE)
    private int correctAnswersCounter;
    private GameStatus status;
    private int result = 0;
    private int currentQuestionNumber = 0;
    // hints
    private boolean halfHintUsed = false;
    private boolean threeLeftHintUsed = false;
    private boolean revelCountHintUsed = false;

    public QuizQuestionModel getNextQuestion() {
        return getByIndex(currentQuestionNumber++);
    }

    public QuizQuestionModel getCurrentQuestion() {
        return getByIndex(currentQuestionNumber);
    }

    public int calculateAndGetAnswersResult() {
        this.result = (correctAnswersCounter * 100) / getQuestionsCount();
        return this.result;
    }

    public String getAnswersStatistic() {
        return correctAnswersCounter + "/" + getQuestionsCount();
    }

    public int getQuestionsCount() {
        return questions.size();
    }

    public void incrementCorrectAnswersCounter() {
        correctAnswersCounter++;
    }

    private QuizQuestionModel getByIndex(int index) {
        if (getQuestionsCount() == index) {
            return null;
        }
        return new ArrayList<>(questions).get(index);
    }
}
