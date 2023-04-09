package org.rsinitsyn.quiz.service;

import com.google.common.collect.Iterables;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@Data
public class CleverestGameState {
    private String createdBy;
    private Map<String, UserGameState> users = new HashMap<>();
    private List<QuizQuestionModel> firstQuestions = new ArrayList<>();
    private List<QuizQuestionModel> secondQuestions = new ArrayList<>();
    private Map<String, List<QuizQuestionModel>> thirdQuestions = new HashMap<>();
    private List<QuizQuestionModel> specialQuestions = new ArrayList<>();
    private Map<Integer, String> roundRules = new HashMap<>();

    // mutable
    private Iterator<UserGameState> usersToAnswer = null;
    private Map<QuizQuestionModel, List<UserGameState>> history = new LinkedHashMap<>();
    private LocalDateTime questionRenderTime;
    private int specialQuestionsNumber;
    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<List<QuizQuestionModel>> currRoundQuestionsSource = null;

    public void init(List<QuizQuestionModel> firstRound,
                     List<QuizQuestionModel> secondRound,
                     Map<String, List<QuizQuestionModel>> thirdRound,
                     List<QuizQuestionModel> special) {
        this.firstQuestions = firstRound;
        this.secondQuestions = secondRound;
        this.thirdQuestions = thirdRound;
        this.specialQuestions = special;
        currRoundQuestionsSource = () -> firstQuestions;
        initRoundRules();
    }

    private void initRoundRules() {
        roundRules.put(1, "В первом раунде будут вопросы на общие темы и 4 варианта ответов. Как только все выберут ответ мы увидим результаты и посчитаем баллы.");
        roundRules.put(2, "Во втором раунде будут вопросы на разные темы но уже без вариантов ответа. Как только все напишут свой ответ мы увидим результаты и посчитаем баллы.");
        roundRules.put(3, "В третьем раунде по очереди нужно выбрать тему и ответить на вопрос устно. После этого мы посчитаем баллы.");
    }

    public void updateHistory(QuizQuestionModel key, UserGameState currUserState) {
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
                        .comparingLong(UserGameState::getLastResponseTime)
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
        refreshQuestionRenderTime();
        return currRoundQuestionsSource.get().get(questionNumber);
    }

    public void refreshQuestionRenderTime() {
        questionRenderTime = LocalDateTime.now();
    }

    public boolean prepareNextRoundAndCheckIsLast() {
        roundNumber++;
        questionNumber = 0;
        if (roundNumber == 2) {
            currRoundQuestionsSource = () -> secondQuestions;
        } else if (roundNumber == 3) {
            usersToAnswer = Iterables.cycle(getSortedByScoreUsers().values()).iterator();
        }
        return roundNumber > 3;
    }

    public boolean prepareNextQuestionAndCheckIsLast() {
        questionNumber++;
        return questionNumber == currRoundQuestionsSource.get().size();
    }

    public void submitAnswerAndIncrease(String username, QuizQuestionModel.QuizAnswerModel answer) {
        UserGameState userGameState = users.get(username);
        if (userGameState.isAnswerGiven()) {
            return;
        }
        userGameState.submitLatestAnswer(answer.getText(), questionRenderTime);
        if (answer.isCorrect()) {
            userGameState.increaseScore();
        }
    }

    public void submitAnswer(String username, String textAnswer) {
        UserGameState userGameState = users.get(username);
        if (userGameState.isAnswerGiven()) {
            return;
        }
        userGameState.submitLatestAnswer(textAnswer, questionRenderTime);
    }

    public boolean areAllUsersAnswered() {
        return users.values().stream().allMatch(UserGameState::isAnswerGiven);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserGameState implements Comparable<UserGameState> {
        private String username;
        private String color;
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
        private String winnerBet = "";
        private String loserBet = "";

        public void submitLatestAnswer(String answerText, LocalDateTime questionRenderTime) {
            lastAnswerText = answerText;
            answerGiven = true;
            lastAnswerTime = LocalDateTime.now();
            lastResponseTime = ChronoUnit.MILLIS.between(
                    questionRenderTime,
                    lastAnswerTime);
        }

        public void prepareForNext() {
            lastWasCorrect = false;
            lastAnswerText = "";
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
                    color,
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
