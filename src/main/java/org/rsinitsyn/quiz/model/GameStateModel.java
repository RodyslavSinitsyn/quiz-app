package org.rsinitsyn.quiz.model;

import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * Represents the state of one specific quiz game
 */
@Data
public class GameStateModel {
    @Length(min = 1, max = 30)
    private String gameName;
    @Length(min = 1, max = 30)
    private String playerName;
    private Set<QuizQuestionModel> questions = new HashSet<>();
    private Set<QuizQuestionModel> correct = new HashSet<>();
}
