package org.rsinitsyn.quiz.view;

import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.rsinitsyn.quiz.utils.QuizUtils.divide;

public interface GameDetailsView {

    UUID getId();

    GameStatus getStatus();

    String getCreatedBy();

    GameType getType();

    String getName();

    String[] getPlayerNames();

    int getAnsweredQuestions();

    int getTotalQuestions();

    int getCorrectAnswers();

    LocalDateTime getCreationDate();

    LocalDateTime getFinishDate();

    default boolean oldSource() {
        return false;
    }

    default List<String> playerNames() {
        return new ArrayList<>(List.of(getPlayerNames()));
    }

    default String questionsResult() {
        return "%d/%d".formatted(getAnsweredQuestions(), getTotalQuestions());
    }

    default double resultPercentage() {
        if (getTotalQuestions() == 0) {
            return 0;
        }
        return divide(getCorrectAnswers() * 100, getTotalQuestions(), 2);
    }
}
