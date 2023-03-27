package org.rsinitsyn.quiz.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
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
    @Length(min = 1, max = 30)
    private String gameName = "Test";
    @Length(min = 1, max = 30)
    private String playerName = "Test";
    private boolean answerOptionsEnabled;
    private boolean timerEnabled;
    private Set<QuizQuestionModel> questions = new HashSet<>();

    // state props
    private Set<QuizQuestionModel> correct = new HashSet<>();
    private GameStatus status;
    private int result = 0;
    private int currentQuestionNumber = 0;

    public QuizQuestionModel getNextQuestion() {
        return getByIndex(currentQuestionNumber++);
    }

    public QuizQuestionModel getCurrentQuestion() {
        return getByIndex(currentQuestionNumber);
    }

    public int calculateAndGetAnswersResult() {
        this.result = (getCorrect().size() * 100) / getQuestionsCount();
        return this.result;
    }

    public String getAnswersStatistic() {
        return correct.size() + "/" + questions.size();
    }

    public int getQuestionsCount() {
        return questions.size();
    }

    private QuizQuestionModel getByIndex(int index) {
        if (getQuestionsCount() == index) {
            return null;
        }
        return new ArrayList<>(questions).get(index);
    }
}
