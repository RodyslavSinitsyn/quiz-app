package org.rsinitsyn.quiz.service;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.UserEvent;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleverestBroadcaster {

    private final Map<String, CleverestGameState> gameStateMap = new ConcurrentHashMap<>();
    private final Map<String, ComponentEventBus> eventBuses = new ConcurrentHashMap<>();

    public boolean stateExists(String gameId) {
        return gameStateMap.containsKey(gameId);
    }

    public CleverestGameState getState(String gameId) {
        return gameStateMap.get(gameId);
    }

    public void cleanState(String gameId) {
        gameStateMap.remove(gameId);
        eventBuses.remove(gameId);
    }

    public void createState(String gameId,
                            String createdBy,
                            List<QuestionModel> firstRound,
                            List<QuestionModel> secondRound,
                            List<QuestionModel> thirdRound) {
        log.info("Create game state: {}", gameId);
        final var categoriesAndQuestions = thirdRound.stream()
                .collect(Collectors.groupingBy(QuestionModel::getCategoryName,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                questionModels -> {
                                    final var questionPoints = new AtomicInteger(1);
                                    return questionModels.stream()
                                            .peek(q -> q.setPoints(questionPoints.getAndIncrement()))
                                            .toList();
                                }
                        )));
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
                                  InputStream photo,
                                  String winnerBet,
                                  String loserBet) {
        CleverestGameState gameState = getState(gameId);
        final var userState = gameState.addOrUpdateUser(gameId, username, userColor,
                ofNullable(photo).map(s -> {
                    try {
                        return s.readAllBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).orElse(null), winnerBet, loserBet);
        eventBuses.get(gameId).fireEvent(new UserJoinedEvent(gameId, userState, gameState.getAllUserStates()));
    }

    public void sendUserBetEvent(String gameId, String username, String userBet, boolean winner) {
        UserGameState userGameState = getState(gameId).getUserState(username);
        userGameState.updateBet(userBet, winner, false);
        eventBuses.get(gameId).fireEvent(new UserBetEvent(gameId, userGameState, userBet, getState(gameId).getAllUserStates()));
    }

    //    AllPlayersReadyEvent
    public void sendUsersReadyEvent(String gameId) {
        log.info("Players ready, start game: {}", gameId);
        eventBuses.get(gameId).fireEvent(new AllUsersReadyEvent(gameId, getState(gameId).getAllUsernames()));
    }

    public void sendSaveUsersAnswersEvent(String gameId, QuestionModel question) {
        CleverestGameState state = getState(gameId);
        var usersWhoAnswered = state.usersWhoAnswered();

        usersWhoAnswered.forEach(userState -> state.putUserStateToHistory(question, userState));
        log.info("History updated. Users gave answers count: {}. Save answers to DB: {}", usersWhoAnswered.size(), gameId);
        eventBuses.get(gameId).fireEvent(new SaveUsersAnswersEvent(
                gameId,
                question,
                getState(gameId).getHistory().get(question)));

        state.usersCleanState();
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
        final var userGameState = getState(gameId).getUserState(username);
        eventBuses.get(gameId).fireEvent(
                new UserAnsweredEvent(gameId,
                        userGameState.getUsername(),
                        userGameState.getLastResponseTimeSec(),
                        getState(gameId).getRoundNumber()));

        if (getState(gameId).areAllUsersAnswered()
                && getState(gameId).getRoundNumber() != 3) {
            sendEventWhenAllAnswered(gameId, questionModel);
        }
    }

    public void sendNewRoundEvent(String gameId) {
        int currRound = getState(gameId).getRoundNumber();
        eventBuses.get(gameId).fireEvent(new RoundInfoEvent(gameId,
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
                        new RoundInfoEvent(
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
                userToAnswer.getUsername()
        ));
    }

    @Getter
    @EqualsAndHashCode(of = "gameId", callSuper = false)
    @ToString(of = "gameId", callSuper = false)
    public abstract static class CleverestGameEvent extends ComponentEvent<Div> {
        private final String gameId;

        public CleverestGameEvent(String gameId) {
            super(new Div(), false);
            this.gameId = gameId;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class UserJoinedEvent extends CleverestGameEvent {
        private final UserGameState user;
        private List<UserGameState> allUsers;

        public UserJoinedEvent(String gameId,
                               UserGameState user,
                               List<UserGameState> allUsers) {
            super(gameId);
            this.user = user;
            this.allUsers = allUsers;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class UserBetEvent extends CleverestGameEvent {
        private final UserGameState user;
        private final String userToBet;
        private List<UserGameState> allUsers;

        public UserBetEvent(String gameId,
                            UserGameState user,
                            String userToBet,
                            List<UserGameState> allUsers) {
            super(gameId);
            this.user = user;
            this.userToBet = userToBet;
            this.allUsers = allUsers;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class UpdatePersonalScoreEvent extends CleverestGameEvent {
        public UpdatePersonalScoreEvent(String gameId) {
            super(gameId);
        }
    }

    @Getter
    @EqualsAndHashCode(of = "usernames", callSuper = false)
    @ToString(of = "usernames", callSuper = true)
    public static class AllUsersReadyEvent extends CleverestGameEvent {
        private final Set<String> usernames;

        public AllUsersReadyEvent(String gameId, Set<String> usernames) {
            super(gameId);
            this.usernames = usernames;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString
    public static class UserAnsweredEvent extends CleverestGameEvent implements UserEvent {
        private final String username;
        private final String lastResponseTimeSec;
        private final int roundNumber;

        public UserAnsweredEvent(final String gameId,
                                 final String username,
                                 final String lastResponseTimeSec,
                                 final int roundNumber) {
            super(gameId);
            this.username = username;
            this.lastResponseTimeSec = lastResponseTimeSec;
            this.roundNumber = roundNumber;
        }

        @Override
        public String username() {
            return username;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class AllUsersAnsweredEvent extends CleverestGameEvent {
        private final QuestionModel question;
        private final boolean roundOver;
        private final boolean roundsOver;
        private final int currentRound;
        private final int revealScoreAfter;

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
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class RoundInfoEvent extends CleverestGameEvent {
        private final int roundNumber;
        private final String rules;

        public RoundInfoEvent(String gameId, int roundNumber, String rules) {
            super(gameId);
            this.roundNumber = roundNumber;
            this.rules = rules;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class GetQuestionEvent extends CleverestGameEvent {
        private final QuestionModel question;
        private final int questionNumber;
        private final int totalQuestionsInRound;
        private final int roundNumber;

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
    @EqualsAndHashCode(of = {"question", "userStateSnapshots"}, callSuper = true)
    @ToString(of = {"question", "userStateSnapshots"}, callSuper = true)
    public static class SaveUsersAnswersEvent extends CleverestGameEvent {
        private final QuestionModel question;
        private final List<UserStateSnapshot> userStateSnapshots;

        public SaveUsersAnswersEvent(String gameId,
                                     QuestionModel question,
                                     List<UserStateSnapshot> userStateSnapshots) {
            super(gameId);
            this.question = question;
            this.userStateSnapshots = userStateSnapshots;
        }
    }

    @Getter
    @EqualsAndHashCode(of = {"question", "username", "grade"}, callSuper = true)
    @ToString(of = {"question", "username", "grade"}, callSuper = true)
    public static class QuestionGradedEvent extends CleverestGameEvent {
        private final QuestionModel question;
        private final String username;
        private final int grade;

        public QuestionGradedEvent(String gameId, QuestionModel question, String username, int grade) {
            super(gameId);
            this.question = question;
            this.username = username;
            this.grade = grade;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class GameFinishedEvent extends CleverestGameEvent {
        public GameFinishedEvent(String gameId) {
            super(gameId);
        }
    }

    @Getter
    @EqualsAndHashCode(of = {"question", "username"}, callSuper = true)
    @ToString(of = {"question", "username"}, callSuper = true)
    public static class QuestionChoosenEvent extends CleverestGameEvent {
        private final QuestionModel question;
        private final String username;

        public QuestionChoosenEvent(String gameId,
                                    QuestionModel question,
                                    String username) {
            super(gameId);
            this.question = question;
            this.username = username;
        }
    }

    public <T extends CleverestGameEvent> Registration subscribe(String gameId,
                                                                 Class<T> eventType,
                                                                 ComponentEventListener<T> listener) {
        final var eventBus = eventBuses.computeIfAbsent(gameId, bus -> new ComponentEventBus(new Div()));
        Registration[] regHolder = new Registration[1];
        regHolder[0] = eventBus.addListener(eventType, event -> {
            log.info("[FIX][Broadcaster={}] Execute [{}] reg=[{}], User [{}], UI [{}], EventBus size[{}]",
                    this.hashCode(), eventType.getSimpleName(),
                    regHolder[0].hashCode(),
                    getLoggedUser(),
                    event.getSource().getUI().map(Objects::hashCode).orElse(-1),
                    eventBuses.get(gameId).getListeners(CleverestGameEvent.class).size());
            listener.onComponentEvent(event);
        });
        Registration reg = regHolder[0];
        log.info("[FIX][Broadcaster={}] Registered [{}], User [{}], Registration [{}], EventBus size[{}]",
                this.hashCode(), eventType.getSimpleName(), getLoggedUser(), reg.hashCode(), eventBuses.get(gameId).getListeners(CleverestGameEvent.class).size());
        return reg;
    }

    void registerEventBus(String gameId, ComponentEventBus bus) {
        eventBuses.put(gameId, bus);
    }
}
