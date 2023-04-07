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
import lombok.Data;
import lombok.Setter;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@Data
public class CleverestGameState {
    private String createdBy;
    private Map<String, UserGameState> users = new HashMap<>();
    private Set<QuizQuestionModel> roundFirstQuestions = new HashSet<>();
    private Set<QuizQuestionModel> roundSecondQuestions = new HashSet<>();
    private Map<String, List<QuizQuestionModel>> roundThirdQuestions = new HashMap<>();
    private List<QuizQuestionModel> specialQuestions = new ArrayList<>();

    private LocalDateTime questionRenderTime;
    private int specialQuestionsNumber;
    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<Set<QuizQuestionModel>> currRoundQuestionsSource = null;

    public void init(Set<QuizQuestionModel> firstRound,
                     Set<QuizQuestionModel> secondRound,
                     Map<String, List<QuizQuestionModel>> thirdRound,
                     Set<QuizQuestionModel> special) {
        this.roundFirstQuestions = firstRound;
        this.roundSecondQuestions = secondRound;
        this.roundThirdQuestions = thirdRound;
        this.specialQuestions = new ArrayList<>(special);
        currRoundQuestionsSource = () -> roundFirstQuestions;
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
            currRoundQuestionsSource = () -> roundSecondQuestions;
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
        return users.values().stream().allMatch(UserGameState::isCurrAnswered);
    }


    @Data
    public static class UserGameState implements Comparable<UserGameState> {
        @Setter(AccessLevel.NONE)
        private String latestAnswer;
        @Setter(AccessLevel.NONE)
        private LocalDateTime lastAnswerTime;
        private long latestResponseTimeMs;
        @Setter(AccessLevel.NONE)
        private int score = 0;
        @Setter(AccessLevel.NONE)
        private boolean currAnswered;
        private String winnerBet;
        private String loserBet;

        public long setAndGetLatestResponseTime(LocalDateTime questionRenderTime) {
            latestResponseTimeMs = ChronoUnit.MILLIS.between(
                    questionRenderTime,
                    lastAnswerTime
            );
            return latestResponseTimeMs;
        }

        public void submitLatestAnswer(String answert) {
            latestAnswer = answert;
            currAnswered = true;
            lastAnswerTime = LocalDateTime.now();
        }

        public void prepareForNext() {
            latestAnswer = null;
            currAnswered = false;
            lastAnswerTime = null;
            latestResponseTimeMs = 0;
        }

        public void increaseScore() {
            score++;
        }

        @Override
        public int compareTo(UserGameState other) {
            return Comparator.comparingInt(UserGameState::getScore)
                    .reversed()
                    .compare(this, other);
        }
    }
}
