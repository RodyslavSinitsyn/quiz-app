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
import lombok.Getter;
import lombok.Setter;
import org.rsinitsyn.quiz.model.QuestionModel;

@Getter
public class CleverestGameState {

    private final Map<String, UserGameState> users = new HashMap<>();
    private final Map<Integer, String> roundRules = new HashMap<>();
    private final Map<QuestionModel, List<UserGameState>> history = new LinkedHashMap<>();

    @Setter
    private String createdBy;
    private List<QuestionModel> firstQuestions = new ArrayList<>();
    private List<QuestionModel> secondQuestions = new ArrayList<>();
    private Map<String, List<QuestionModel>> thirdQuestions = new HashMap<>();

    // mutable
    private Iterator<UserGameState> usersToAnswer = null;
    private LocalDateTime questionRenderedTime;
    private int roundNumber = 1;
    private int questionNumber = 0;
    private Supplier<List<QuestionModel>> currRoundQuestionsSource = null;

    public void init(List<QuestionModel> firstRound,
                     List<QuestionModel> secondRound,
                     Map<String, List<QuestionModel>> thirdRound) {
        this.firstQuestions = firstRound;
        this.secondQuestions = secondRound;
        this.thirdQuestions = thirdRound;
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

    public void prepareUsersForThirdRound() {
        usersToAnswer = Iterables.cycle(getSortedByScoreUsers().values()).iterator();
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
