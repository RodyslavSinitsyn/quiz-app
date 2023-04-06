package org.rsinitsyn.quiz.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import lombok.Data;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@Data
public class CleverestGameState {
    private Map<String, UserGameState> users = new HashMap<>();
    private Set<QuizQuestionModel> roundFirstQuestions = new HashSet<>();
    private Set<QuizQuestionModel> roundSecondQuestions = new HashSet<>();

    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<Set<QuizQuestionModel>> currRoundQuestionsSource = null;

    public void init(Set<QuizQuestionModel> firstRound, Set<QuizQuestionModel> secondRound) {
        this.roundFirstQuestions = firstRound;
        this.roundSecondQuestions = secondRound;
        currRoundQuestionsSource = () -> roundFirstQuestions;
    }

    public QuizQuestionModel getCurrent() {
        if (questionNumber == currRoundQuestionsSource.get().size()) {
            return null;
        }
        return new ArrayList<>(currRoundQuestionsSource.get()).get(questionNumber);
    }

    public boolean prepareNextRoundAndCheckIsLast() {
        roundNumber++;
        questionNumber = 0;
        if (roundNumber == 2) {
            currRoundQuestionsSource = () -> roundSecondQuestions;
        }
        return roundNumber == 3;
    }

    public boolean prepareNextQuestionAndCheckIsLast() {
        questionNumber++;
        return questionNumber == currRoundQuestionsSource.get().size();
    }

    public void submitAnswer(String username, QuizQuestionModel.QuizAnswerModel answer) {
        UserGameState userGameState = users.get(username);
        userGameState.setCurrAnswered(true);
        userGameState.setLatestAnswer(answer.getText());
        if (answer.isCorrect()) {
            userGameState.increaseScore();
        }
    }

    public void submitAnswer(String username, String textAnswer) {
        UserGameState userGameState = users.get(username);
        userGameState.setCurrAnswered(true);
        userGameState.setLatestAnswer(textAnswer);
    }

    public boolean areAllUsersAnswered() {
        return users.values().stream().allMatch(UserGameState::isCurrAnswered);
    }


    @Data
    public static class UserGameState {
        private String latestAnswer;
        private int score = 0;
        private boolean currAnswered;

        public void increaseScore() {
            score++;
        }
    }
}
