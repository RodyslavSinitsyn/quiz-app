package org.rsinitsyn.quiz.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.model.UserStatsModel;

@Data
public class CleverestGameState {
    private String createdBy;
    private Map<String, UserGameState> users = new HashMap<>();
    private Set<QuizQuestionModel> firstQuestions = new HashSet<>();
    private Set<QuizQuestionModel> secondQuestions = new HashSet<>();
    private Map<String, List<QuizQuestionModel>> thirdQuestions = new HashMap<>();
    private List<QuizQuestionModel> specialQuestions = new ArrayList<>();

    // mutable
    private Map<QuizQuestionModel, List<UserGameState>> history = new LinkedHashMap<>();
    private LocalDateTime questionRenderTime;
    private int specialQuestionsNumber;
    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<Set<QuizQuestionModel>> currRoundQuestionsSource = null;

    public void init(Set<QuizQuestionModel> firstRound,
                     Set<QuizQuestionModel> secondRound,
                     Map<String, List<QuizQuestionModel>> thirdRound,
                     Set<QuizQuestionModel> special) {
        this.firstQuestions = firstRound;
        this.secondQuestions = secondRound;
        this.thirdQuestions = thirdRound;
        this.specialQuestions = new ArrayList<>(special);
        currRoundQuestionsSource = () -> firstQuestions;
    }

    public void updateHistory(QuizQuestionModel key, String username) {
        UserGameState currUserState = users.get(username);

        List<UserGameState> states = history.get(key);
        if (states == null || states.isEmpty()) {
            List<UserGameState> temp = new ArrayList<>();
            temp.add(currUserState.copy());
            history.put(key, temp);
        } else {
            states.add(currUserState.copy());
        }
    }

    public Map<String, UserGameState> getSortedByScoreUsers() {
        return users.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e2,
                        LinkedHashMap::new));
    }

    public Map<String, UserGameState> getSortedByResponseTimeUsers() {
        return users.entrySet().stream()
                .sorted(Map.Entry.comparingByValue((s1, s2) -> Comparator
                        .<UserGameState>comparingLong(val -> val.setAndGetLatestResponseTime(questionRenderTime))
                        .compare(s1, s2)))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e2,
                        LinkedHashMap::new));
    }

    public boolean specialQuestionShouldAppear() {
//        int twoRoundsSize = roundFirstQuestions.size() + roundSecondQuestions.size();
//        int delta = twoRoundsSize / specialQuestions.size();
//        return questionNumber % delta == 0;
        return questionNumber != 0 && questionNumber % 2 == 0;
    }

    public QuizQuestionModel getSpecial() {
        if (specialQuestionsNumber == specialQuestions.size()) {
            return null;
        }
        return specialQuestions.get(specialQuestionsNumber++);
    }

    public QuizQuestionModel getCurrent() {
        if (questionNumber == currRoundQuestionsSource.get().size()) {
            return null;
        }
        questionRenderTime = LocalDateTime.now();
        return new ArrayList<>(currRoundQuestionsSource.get()).get(questionNumber);
    }

    public boolean prepareNextRoundAndCheckIsLast() {
        roundNumber++;
        questionNumber = 0;
        if (roundNumber == 2) {
            currRoundQuestionsSource = () -> secondQuestions;
        }
        return roundNumber > 3;
    }

    public boolean prepareNextQuestionAndCheckIsLast() {
        questionNumber++;
        return questionNumber == currRoundQuestionsSource.get().size();
    }

    public void submitAnswerAndIncrease(String username, QuizQuestionModel.QuizAnswerModel answer) {
        UserGameState userGameState = users.get(username);
        userGameState.submitLatestAnswer(answer.getText());
        if (answer.isCorrect()) {
            userGameState.increaseScore();
        }
    }

    public void submitAnswer(String username, String textAnswer) {
        UserGameState userGameState = users.get(username);
        userGameState.submitLatestAnswer(textAnswer);
    }

    public boolean areAllUsersAnswered() {
        return users.values().stream().allMatch(UserGameState::isAnswerGiven);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserGameState implements Comparable<UserGameState> {
        private String username;
        private boolean lastWasCorrect;
        @Setter(AccessLevel.NONE)
        private String lastAnswerText;
        @Setter(AccessLevel.NONE)
        private LocalDateTime lastAnswerTime;
        private long lastResponseTime;
        @Setter(AccessLevel.NONE)
        private int score = 0;
        @Setter(AccessLevel.NONE)
        private boolean answerGiven;
        private String winnerBet;
        private String loserBet;

        public long setAndGetLatestResponseTime(LocalDateTime questionRenderTime) {
            lastResponseTime = ChronoUnit.MILLIS.between(
                    questionRenderTime,
                    lastAnswerTime
            );
            return lastResponseTime;
        }

        public void submitLatestAnswer(String answerText) {
            lastAnswerText = answerText;
            answerGiven = true;
            lastAnswerTime = LocalDateTime.now();
        }

        public void prepareForNext() {
            lastWasCorrect = false;
            lastAnswerText = null;
            answerGiven = false;
            lastAnswerTime = null;
            lastResponseTime = 0;
        }

        public void increaseScore() {
            score++;
            lastWasCorrect = true;
        }

        @Override
        public int compareTo(UserGameState other) {
            return Comparator.comparingInt(UserGameState::getScore)
                    .reversed()
                    .compare(this, other);
        }

        public UserGameState copy() {
            return new UserGameState(
                    username,
                    lastWasCorrect,
                    lastAnswerText,
                    lastAnswerTime,
                    lastResponseTime,
                    score,
                    answerGiven,
                    winnerBet,
                    loserBet);
        }
    }
}
