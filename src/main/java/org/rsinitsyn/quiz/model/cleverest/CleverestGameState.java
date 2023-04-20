package org.rsinitsyn.quiz.model.cleverest;

import com.google.common.collect.Iterables;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Data;
import org.rsinitsyn.quiz.model.QuestionModel;

@Data
public class CleverestGameState {
    private String createdBy;
    private Map<String, UserGameState> users = new HashMap<>();
    private List<QuestionModel> firstQuestions = new ArrayList<>();
    private List<QuestionModel> secondQuestions = new ArrayList<>();
    private Map<String, List<QuestionModel>> thirdQuestions = new HashMap<>();
    private List<QuestionModel> specialQuestions = new ArrayList<>();
    private Map<Integer, String> roundRules = new HashMap<>();

    // mutable
    private Iterator<UserGameState> usersToAnswer = null;
    private Map<QuestionModel, List<UserGameState>> history = new LinkedHashMap<>();
    private LocalDateTime questionRenderTime;
    private int specialQuestionsNumber;
    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<List<QuestionModel>> currRoundQuestionsSource = null;

    public void init(List<QuestionModel> firstRound,
                     List<QuestionModel> secondRound,
                     Map<String, List<QuestionModel>> thirdRound,
                     List<QuestionModel> special) {
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

    public void updateHistory(QuestionModel key, UserGameState currUserState) {
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

    public QuestionModel getSpecial() {
        if (specialQuestionsNumber == specialQuestions.size()) {
            return null;
        }
        return specialQuestions.get(specialQuestionsNumber++);
    }

    public QuestionModel getCurrent() {
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
        }
        return roundNumber > 3;
    }

    public void prepareUsersForThirdRound() {
        usersToAnswer = Iterables.cycle(getSortedByScoreUsers().values()).iterator();
    }

    public boolean prepareNextQuestionAndCheckIsLast() {
        questionNumber++;
        return questionNumber == currRoundQuestionsSource.get().size();
    }

    public void submitAnswerAndIncrease(String username, QuestionModel.AnswerModel answer) {
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

    public void updateUserPositions() {
        Map<String, UserGameState> sortedByScore = getSortedByScoreUsers();
        AtomicInteger pos = new AtomicInteger(1);
        AtomicInteger prevScoreHolder = new AtomicInteger(0);
        sortedByScore.forEach((username, userGameState) -> {
            if (userGameState.totalScore() < prevScoreHolder.get()) {
                pos.incrementAndGet();
            }
            userGameState.setLastPosition(pos.get());
            prevScoreHolder.set(userGameState.totalScore());
        });
    }

    public void calculateUsersStatistic() {
        Collection<UserGameState> latestStates = users.values();
        int bottomPos = latestStates.stream()
                .mapToInt(UserGameState::getLastPosition)
                .max().orElse(latestStates.size());
        int topPos = 1;

        Set<String> highestScoreUsers = latestStates.stream()
                .filter(u -> u.getLastPosition() == topPos)
                .map(UserGameState::getUsername)
                .collect(Collectors.toSet());

        Set<String> lowestScoreUsers = latestStates.stream()
                .filter(u -> u.getLastPosition() == bottomPos)
                .map(UserGameState::getUsername)
                .collect(Collectors.toSet());

        latestStates.stream()
                .filter(u -> highestScoreUsers.contains(u.winnerBet().getKey()))
                .forEach(u -> {
                    u.winnerBet().setValue(true);
                    u.increaseBetScore();
                });

        latestStates.stream()
                .filter(u -> lowestScoreUsers.contains(u.loserBet().getKey()))
                .forEach(u -> {
                    u.loserBet().setValue(true);
                    u.increaseBetScore();
                });

        history.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .filter(uState -> uState.getLastResponseTime() > 0)
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.averagingLong(UserGameState::getLastResponseTime)))
                .forEach((userGameState, avgTime) -> {
                    users.get(userGameState.getUsername()).setAvgResponseTime(avgTime);
                });
    }
}
