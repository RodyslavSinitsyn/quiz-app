package org.rsinitsyn.quiz.model.cleverest;

import com.google.common.collect.Iterables;

import java.io.InputStream;
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

import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.UserStateSnapshot;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.rsinitsyn.quiz.model.cleverest.UserGameState.userGameState;

@Getter
public class CleverestGameState {

    @Getter(AccessLevel.NONE)
    private final Map<String, UserGameState> users = new HashMap<>();
    private final String createdBy;
    private final List<QuestionModel> firstQuestions;
    private final List<QuestionModel> secondQuestions;
    private final Map<String, List<QuestionModel>> thirdQuestions;

    private final Map<Integer, String> roundRules = new HashMap<>();
    private final Map<QuestionModel, List<UserStateSnapshot>> history = new LinkedHashMap<>();

    // mutable
    private Iterator<UserGameState> usersToAnswerOrder = null;
    private LocalDateTime questionRenderedTime;
    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<List<QuestionModel>> currRoundQuestionsSource;

    public CleverestGameState(
            String createdBy,
            List<QuestionModel> firstRound,
            List<QuestionModel> secondRound,
            Map<String, List<QuestionModel>> thirdRound) {
        this.createdBy = createdBy;
        this.firstQuestions = firstRound;
        this.secondQuestions = secondRound;
        this.thirdQuestions = thirdRound;
        currRoundQuestionsSource = () -> firstQuestions;
        initRoundRules();
    }

    private void initRoundRules() {
        roundRules.put(1, "В первом раунде будут вопросы на разные темы и 4 варианта ответов.");
        roundRules.put(2, "Во втором раунде будут вопросы на разные темы, без вариантов ответов.");
        roundRules.put(3, "В третьем раунде по очереди нужно выбрать тему и ответить на вопрос. За верный ответ дают баллы, за неверный забирают. Количество баллов зависит от сложности вопроса.");
    }

    public UserGameState addOrUpdateUser(String gameId,
                                         String username,
                                         String userColor,
                                         InputStream photo,
                                         String winnerBet,
                                         String loserBet) {
        users.computeIfAbsent(username, key -> userGameState(username, userColor));
        return users.computeIfPresent(username, (key, userGameState) -> {
            userGameState.updateColor(userColor);
            userGameState.updateBet(defaultIfEmpty(winnerBet, ""), true, false);
            userGameState.updateBet(defaultIfEmpty(loserBet, ""), false, false);
            return userGameState;
        });
    }

    public boolean usersPresent() {
        return MapUtils.isNotEmpty(users);
    }

    public boolean userPresent(String username) {
        return users.containsKey(username);
    }

    public UserGameState getUserState(String username) {
        return users.get(username);
    }

    public Set<String> getAllUsernames() {
        return users.keySet();
    }

    public List<UserGameState> getAllUserStates() {
        return new ArrayList<>(users.values());
    }

    public void putUserStateToHistory(QuestionModel key, UserGameState currUserState) {
        history.computeIfAbsent(key, ignored -> new ArrayList<>(5))
                .add(currUserState.snapshot());
    }

    public List<UserGameState> usersWhoAnswered() {
        return users.values()
                .stream()
                .filter(UserGameState::isAnswerGiven)
                .toList();
    }

    public void usersCleanState() {
        users.values().forEach(UserGameState::prepareForNext);
    }

    public Map<String, UserGameState> usersSortedByScore() {
        return users.entrySet().stream()
                .sorted(comparingByValue())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e2,
                        LinkedHashMap::new));
    }

    public Map<String, UserStateSnapshot> userSnapshotsSortedByResponseTime() {
        return users.entrySet().stream()
                .sorted(comparingByValue((s1, s2) -> Comparator
                        .comparingLong(UserGameState::getLastResponseTimeMs)
                        .compare(s1, s2)))
                .collect(toMap(Map.Entry::getKey,
                        v -> v.getValue().snapshot(),
                        (e1, e2) -> e2,
                        LinkedHashMap::new));
    }

    public QuestionModel getCurrentQuestion() {
        if (questionNumber == currRoundQuestionsSource.get().size()) {
            return null;
        }
        refreshQuestionRenderedTime();
        return currRoundQuestionsSource.get().get(questionNumber);
    }

    public void refreshQuestionRenderedTime() {
        questionRenderedTime = LocalDateTime.now();
    }

    public boolean prepareNextRoundAndCheckIsLast() {
        roundNumber++;
        questionNumber = 0;
        if (roundNumber == 2) {
            currRoundQuestionsSource = () -> secondQuestions;
        }
        return roundNumber > 3;
    }

    public void prepareUsersToAnswerOrder() {
        usersToAnswerOrder = Iterables.cycle(usersSortedByScore().values()).iterator();
    }

    public boolean prepareNextQuestionAndCheckIsLast() {
        questionNumber++;
        return questionNumber == currRoundQuestionsSource.get().size();
    }

    public void submitAnswer(String username,
                             String answerAsText,
                             Supplier<Boolean> isCorrect) {
        UserGameState userGameState = users.get(username);
        if (userGameState.isAnswerGiven()) {
            return;
        }
        userGameState.submitLatestAnswer(answerAsText, questionRenderedTime);
        if (isCorrect.get()) {
            userGameState.increaseScore();
        }
    }

    public boolean areAllUsersAnswered() {
        return users.values().stream().allMatch(UserGameState::isAnswerGiven);
    }

    public void updateUserPositions() {
        Map<String, UserGameState> sortedByScore = usersSortedByScore();
        AtomicInteger pos = new AtomicInteger(1);
        AtomicInteger prevScoreHolder = new AtomicInteger(0);
        sortedByScore.forEach((username, userGameState) -> {
            if (userGameState.totalScore() < prevScoreHolder.get()) {
                pos.incrementAndGet();
            }
            userGameState.setLastPosition(pos.get()); // todo: remove setter
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
                .filter(uState -> uState.lastResponseTimeMs() > 0)
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.averagingLong(UserStateSnapshot::lastResponseTimeMs)))
                .forEach((snapshot, avgTime) -> {
                    users.get(snapshot.username()).setAvgResponseTime(avgTime); // todo: remove setter
                });
    }

    public int getQuestionsLeftToRevealScoreTable() {
        final int CHUNK_SIZE = 3;
        if (currRoundQuestionsSource.get().size() <= CHUNK_SIZE) {
            return 0;
        }
        if (currRoundQuestionsSource.get().size() == questionNumber + 1) {
            return 0;
        }
        int currChunk = currRoundQuestionsSource.get().size() / CHUNK_SIZE;
        int questionsAndChunkDiff = (questionNumber / currChunk) + 1;
        currChunk = currChunk * questionsAndChunkDiff;
        return currChunk - (questionNumber + 1);
    }
}
