package org.rsinitsyn.quiz.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * Represents the state of one specific quiz game
 */
// TODO Split to *SettingsBindingModel and *StateModel
@Data
public class QuizGameStateModel {
    @Length(min = 1, max = 30)
    private String gameName;
    @Length(min = 1, max = 30)
    private String playerName;
    private boolean answerOptionsEnabled;
    private boolean timerEnabled;

    // state props
    private Set<QuizQuestionModel> questions = new HashSet<>();
    private Set<QuizQuestionModel> correct = new HashSet<>();
    private boolean finished = false;
}
