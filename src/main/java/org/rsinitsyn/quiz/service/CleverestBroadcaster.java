package org.rsinitsyn.quiz.service;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CleverestBroadcaster {

    private final Map<String, CleverestGameState> gameStateMap = new ConcurrentHashMap<>();
    private final Map<String, ComponentEventBus> eventBuses = new ConcurrentHashMap<>();

    public CleverestGameState getState(String gameId) {
        return gameStateMap.get(gameId);
    }

    public void createState(String gameId,
                            String createdBy,
                            List<QuestionModel> firstRound,
                            List<QuestionModel> secondRound,
                            List<QuestionModel> thirdRound) {
        log.info("Create game state: {}", gameId);
        Map<String, List<QuestionModel>> categoriesAndQuestions = thirdRound.stream()
                .collect(Collectors.groupingBy(QuestionModel::getCategoryName,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                questionModels -> {
                                    AtomicInteger questionPoints = new AtomicInteger(1);
                                    return questionModels.stream()
                                            .peek(q -> q.setPoints(questionPoints.getAndIncrement()))
                                            .toList();
                                }
                        )));
        Collections.shuffle(firstRound);
        Collections.shuffle(secondRound);
        gameStateMap.put(gameId,
                new CleverestGameState(
                        createdBy,
                        firstRound,
                        secondRound,
                        categoriesAndQuestions)
        );
    }

    // UserJoinedEvent
    public void sendJoinUserEvent(String gameId,
                                  String username,
                                  String userColor,
                                  String winnerBet,
                                  String loserBet) {
        CleverestGameState gameState = getState(gameId);

        gameState.getUsers().computeIfAbsent(username, s -> {
            var state = new UserGameState();
            state.setUsername(username);
            state.setColor(userColor);
            return state;
        });

        gameState.getUsers().computeIfPresent(username, (key, userGameState) -> {
            userGameState.setColor(userColor);
            userGameState.updateBet(StringUtils.defaultIfEmpty(winnerBet, ""), true, false);
            userGameState.updateBet(StringUtils.defaultIfEmpty(loserBet, ""), false, false);
            return userGameState;
        });

        eventBuses.get(gameId).fireEvent(new UserJoinedEvent(gameId, username));
    }


    public void sendBetEvent(String gameId, String username, String userBet, boolean winner) {
        UserGameState userGameState = getState(gameId).getUsers().get(username);
        userGameState.updateBet(userBet, winner, false);
        eventBuses.get(gameId).fireEvent(new UserBetEvent(gameId, username, userBet));
    }

    //    AllPlayersReadyEvent
    public void sendPlayersReadyEvent(String gameId) {
        log.info("Players ready, start game: {}", gameId);
        eventBuses.get(gameId).fireEvent(new AllUsersReadyEvent(gameId, getState(gameId).getUsers().keySet()));
    }

    public void sendSaveUserAnswersEvent(String gameId, QuestionModel question) {
        CleverestGameState state = getState(gameId);
        var usersWhoAnswered = state.getUsers().entrySet()
                .stream()
                .filter(e -> e.getValue().isAnswerGiven())
                .toList();

        usersWhoAnswered.forEach(entry -> state.putUserStateToHistory(question, entry.getValue()));
        log.info("History updated. Users gave answers count: {}. Save answers to DB: {}", usersWhoAnswered.size(), gameId);
        eventBuses.get(gameId).fireEvent(new SaveUserAnswersEvent(
                gameId,
                question,
                getState(gameId).getHistory().get(question)));

        state.getUsers().values().forEach(UserGameState::prepareForNext);
        log.info("All users prepared for question.");
    }


    // SubmitUserAnswerEvent && AllUsersAnsweredEvent
    public void sendSubmitAnswerEventAndCheckScore(String gameId,
                                                   String username,
                                                   QuestionModel questionModel,
                                                   String answerAsText,
                                                   Supplier<Boolean> isCorrect) {
        log.info("User gave answer: {} = {}", username, answerAsText);
        getState(gameId).submitAnswer(username, answerAsText, isCorrect);
        eventBuses.get(gameId).fireEvent(
                new UserAnsweredEvent(gameId, getState(gameId).getUsers().get(username), getState(gameId).getRoundNumber()));

        if (getState(gameId).areAllUsersAnswered()
                && getState(gameId).getRoundNumber() != 3) {
            sendEventWhenAllAnswered(gameId, questionModel);
        }
    }

    public void sendNewRoundEvent(String gameId) {
        int currRound = getState(gameId).getRoundNumber();
        eventBuses.get(gameId).fireEvent(new GetRoundEvent(gameId,
                currRound,
                getState(gameId).getRoundRules().get(currRound)));
    }

    private void sendEventWhenAllAnswered(String gameId, QuestionModel currQuestion) {
        CleverestGameState gameState = getState(gameId);
        int leftToRevealScore = gameState.getQuestionsLeftToRevealScoreTable();
        boolean noMoreQuestionsInRound = gameState.prepareNextQuestionAndCheckIsLast();
        boolean roundsOver = false;
        int currRound = gameState.getRoundNumber();
        if (noMoreQuestionsInRound) {
            roundsOver = gameState.prepareNextRoundAndCheckIsLast();
        }
        log.info("All users answered. CurrQuestionNumber: {}. NextQuestionNumber: {}. TotalQuestons: {}, CurrRound: {}. RoundIsOver: {}. GameOver: {}",
                gameState.getQuestionNumber() - 1,
                gameState.getQuestionNumber(),
                gameState.getCurrRoundQuestionsSource().get().size(),
                currRound,
                noMoreQuestionsInRound,
                roundsOver);
        eventBuses.get(gameId).fireEvent(new AllUsersAnsweredEvent(gameId,
                currQuestion,
                noMoreQuestionsInRound,
                roundsOver,
                currRound,
                leftToRevealScore));
    }

    // GameFinishedEvent
    public void sendFinishGameEvent(String gameId) {
        getState(gameId).calculateUsersStatistic();
        getState(gameId).updateUserPositions();
        log.info("Game finished: {}", gameId);
        eventBuses.get(gameId).fireEvent(new CleverestBroadcaster.GameFinishedEvent(gameId));
    }

    // GetQuestionEvent
    public void sendGetQuestionEvent(String gameId) {
        CleverestGameState gameState = getState(gameId);
        QuestionModel question = gameState.getCurrentQuestion();

        // In case smth went wrong
        if (question == null) {
            log.debug("Get question returns null, the cause could be refreshes. Try to finish game or render next round");
            if (gameState.prepareNextRoundAndCheckIsLast()) {
                eventBuses.get(gameId).fireEvent(
                        new GameFinishedEvent(gameId)
                );
            } else {
                eventBuses.get(gameId).fireEvent(
                        new GetRoundEvent(
                                gameId,
                                gameState.getRoundNumber(),
                                gameState.getRoundRules().get(gameState.getRoundNumber())
                        ));
            }
            return;
        }

        log.info("Sending question for render: {}", question.getText() + " - " + question.getCategoryName());
        eventBuses.get(gameId).fireEvent(new GetQuestionEvent(
                gameId,
                question,
                gameState.getQuestionNumber() + 1,
                gameState.getCurrRoundQuestionsSource().get().size(),
                gameState.getRoundNumber()));
    }

    // UpdatePersonalScoreEvent
    public void sendUpdatePersonalScoreEvent(String gameId) {
        log.info("Updating personal score: {}", gameId);
        eventBuses.get(gameId).fireEvent(new UpdatePersonalScoreEvent(gameId));
    }

    // RenderCategoriesEvent
    public void sendRenderCategoriesEvent(String gameId, QuestionModel question, boolean initial) {
        log.info("Sending  categories for render, {}", gameId);
        CleverestGameState gameState = getState(gameId);
        if (initial) {
            gameState.prepareUsersToAnswerOrder();
        }
        if (question != null) {
            question.setAlreadyAnswered(true);
        }
        if (gameState.getThirdQuestions().values().stream()
                .flatMap(Collection::stream)
                .allMatch(QuestionModel::isAlreadyAnswered)) {
            sendFinishGameEvent(gameId);
            return;
        }
        eventBuses.get(gameId).fireEvent(new RenderCategoriesEvent(
                gameId,
                gameState.getUsersToAnswerOrder().next(),
                gameState.getThirdQuestions()));
    }

    public void sendQuestionGradedEvent(String gameId,
                                        QuestionModel questionModel,
                                        String username,
                                        int grade) {
        eventBuses.get(gameId)
                .fireEvent(new QuestionGradedEvent(
                        gameId,
                        questionModel,
                        username,
                        grade
                ));
    }

    public void sendQuestionChoosenEvent(String gameId, QuestionModel question, UserGameState userToAnswer) {
        getState(gameId).refreshQuestionRenderedTime();
        eventBuses.get(gameId).fireEvent(new QuestionChoosenEvent(
                gameId,
                question,
                userToAnswer
        ));
    }

    @Getter
    public abstract static class CleverestGameEvent extends ComponentEvent<Div> {
        private final String gameId;

        public CleverestGameEvent(String gameId) {
            super(new Div(), false);
            this.gameId = gameId;
        }
    }

    @Getter
    public static class UserJoinedEvent extends CleverestGameEvent {
        private String username;

        public UserJoinedEvent(String gameId,
                               String username) {
            super(gameId);
            this.username = username;
        }
    }

    @Getter
    public static class UserBetEvent extends CleverestGameEvent {
        private String username;
        private String userToBet;

        public UserBetEvent(String gameId,
                            String username,
                            String userToBet) {
            super(gameId);
            this.username = username;
            this.userToBet = userToBet;
        }
    }

    @Getter
    public static class UpdatePersonalScoreEvent extends CleverestGameEvent {
        public UpdatePersonalScoreEvent(String gameId) {
            super(gameId);
        }
    }

    @Getter
    public static class AllUsersReadyEvent extends CleverestGameEvent {
        private Set<String> usernames;

        public AllUsersReadyEvent(String gameId, Set<String> usernames) {
            super(gameId);
            this.usernames = usernames;
        }
    }

    @Getter
    public static class UserAnsweredEvent extends CleverestGameEvent {
        private UserGameState userGameState;
        private int roundNumber;

        public UserAnsweredEvent(String gameId, UserGameState userGameState, int roundNumber) {
            super(gameId);
            this.userGameState = userGameState;
            this.roundNumber = roundNumber;
        }
    }

    @Getter
    public static class AllUsersAnsweredEvent extends CleverestGameEvent {
        private QuestionModel question;
        private boolean roundOver;
        private boolean roundsOver;
        private int currentRound;
        private int revealScoreAfter;

        public AllUsersAnsweredEvent(String gameId,
                                     QuestionModel question,
                                     boolean roundOver,
                                     boolean roundsOver,
                                     int currentRound,
                                     int revealScoreAfter) {
            super(gameId);
            this.question = question;
            this.roundOver = roundOver;
            this.roundsOver = roundsOver;
            this.currentRound = currentRound;
            this.revealScoreAfter = revealScoreAfter;
        }
    }

    @Getter
    public static class GetRoundEvent extends CleverestGameEvent {
        private int roundNumber;
        private String rules;

        public GetRoundEvent(String gameId, int roundNumber, String rules) {
            super(gameId);
            this.roundNumber = roundNumber;
            this.rules = rules;
        }
    }

    @Getter
    public static class GetQuestionEvent extends CleverestGameEvent {
        private QuestionModel question;
        private int questionNumber;
        private int totalQuestionsInRound;
        private int roundNumber;

        public GetQuestionEvent(String gameId,
                                QuestionModel question,
                                int questionNumber,
                                int totalQuestionsInRound,
                                int roundNumber) {
            super(gameId);
            this.question = question;
            this.questionNumber = questionNumber;
            this.totalQuestionsInRound = totalQuestionsInRound;
            this.roundNumber = roundNumber;
        }
    }

    @Getter
    public static class RenderCategoriesEvent extends CleverestGameEvent {
        private UserGameState userToAnswer;
        private Map<String, List<QuestionModel>> data;

        public RenderCategoriesEvent(String gameId,
                                     UserGameState userToAnswer,
                                     Map<String, List<QuestionModel>> data) {
            super(gameId);
            this.userToAnswer = userToAnswer;
            this.data = data;
        }
    }

    @Getter
    public static class SaveUserAnswersEvent extends CleverestGameEvent {
        private QuestionModel question;
        private List<UserGameState> userStates;

        public SaveUserAnswersEvent(String gameId,
                                    QuestionModel question,
                                    List<UserGameState> userStates) {
            super(gameId);
            this.question = question;
            this.userStates = userStates;
        }
    }

    @Getter
    public static class QuestionGradedEvent extends CleverestGameEvent {
        private QuestionModel question;
        private String username;
        private int grade;

        public QuestionGradedEvent(String gameId, QuestionModel question, String username, int grade) {
            super(gameId);
            this.question = question;
            this.username = username;
            this.grade = grade;
        }
    }

    @Getter
    public static class GameFinishedEvent extends CleverestGameEvent {
        public GameFinishedEvent(String gameId) {
            super(gameId);
        }
    }

    @Getter
    public static class QuestionChoosenEvent extends CleverestGameEvent {
        private QuestionModel question;
        private UserGameState userToAnswer;

        public QuestionChoosenEvent(String gameId,
                                    QuestionModel question,
                                    UserGameState userToAnswer) {
            super(gameId);
            this.question = question;
            this.userToAnswer = userToAnswer;
        }
    }

    public <T extends ComponentEvent<?>> Registration subscribe(String gameId,
                                                                Class<T> eventType,
                                                                ComponentEventListener<T> listener) {
        ComponentEventBus eventBus = eventBuses.computeIfAbsent(gameId, bus -> new ComponentEventBus(new Div()));
        return eventBus.addListener(eventType, listener);
    }
}
