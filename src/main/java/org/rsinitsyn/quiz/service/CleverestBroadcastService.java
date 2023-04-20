package org.rsinitsyn.quiz.service;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.springframework.stereotype.Component;

@Component
public class CleverestBroadcastService {

    private final Map<String, CleverestGameState> gameStateMap = new ConcurrentHashMap<>();
    private final Map<String, ComponentEventBus> eventBuses = new ConcurrentHashMap<>();

    public CleverestGameState getState(String gameId) {
        return gameStateMap.get(gameId);
    }

    public void createState(String gameId,
                            String createdBy,
                            List<QuizQuestionModel> firstRound,
                            List<QuizQuestionModel> secondRound,
                            List<QuizQuestionModel> thirdRound,
                            List<QuizQuestionModel> special) {
        CleverestGameState state = new CleverestGameState();
        state.init(firstRound,
                secondRound,
                thirdRound.stream().collect(Collectors.groupingBy(QuizQuestionModel::getCategoryName)),
                special);
        state.setCreatedBy(createdBy);
        gameStateMap.put(gameId, state);
    }

    // UserJoinedEvent
    public void sendJoinUserEvent(String gameId,
                                  String username,
                                  String userColor,
                                  String winnerBet,
                                  String loserBet) {
        CleverestGameState gameState = getState(gameId);

        gameState.getUsers().computeIfAbsent(username, s -> {
            var state = new CleverestGameState.UserGameState();
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
        CleverestGameState.UserGameState userGameState = getState(gameId).getUsers().get(username);
        userGameState.updateBet(userBet, winner, false);
        eventBuses.get(gameId).fireEvent(new UserBetEvent(gameId, username, userBet));
    }

    //    AllPlayersReadyEvent
    public void sendPlayersReadyEvent(String gameId) {
        eventBuses.get(gameId).fireEvent(new AllUsersReadyEvent(gameId, getState(gameId).getUsers().keySet()));
    }

    public void sendUpdateHistoryEvent(String gameId, QuizQuestionModel question) {
        getState(gameId).getUsers().entrySet()
                .stream()
                .filter(e -> e.getValue().isAnswerGiven())
                .forEach(entry -> getState(gameId).updateHistory(question, entry.getValue()));
        eventBuses.get(gameId).fireEvent(new SaveUserAnswersEvent(
                gameId,
                question,
                getState(gameId).getHistory().get(question)));
    }


    // SubmitUserAnswerEvent && AllUsersAnsweredEvent
    public void sendSubmitAnswerEventAndIncreaseScore(String gameId, String username, QuizQuestionModel questionModel, QuizQuestionModel.QuizAnswerModel answer) {
        getState(gameId).submitAnswerAndIncrease(username, answer);
        eventBuses.get(gameId).fireEvent(new UserAnsweredEvent(gameId, username));

        if (getState(gameId).areAllUsersAnswered()) {
            sendEventWhenAllAnswered(gameId, questionModel);
        }
    }

    // SubmitUserAnswerEvent && AllUsersAnsweredEvent
    public void sendSubmitAnswerEvent(String gameId, String username, QuizQuestionModel questionModel, String textAnswer) {
        getState(gameId).submitAnswer(username, textAnswer);
        eventBuses.get(gameId).fireEvent(new UserAnsweredEvent(gameId, username));

        if (getState(gameId).areAllUsersAnswered()) {
            sendEventWhenAllAnswered(gameId, questionModel);
        }
    }

    public void sendNewRoundEvent(String gameId) {
        int currRound = getState(gameId).getRoundNumber();
        eventBuses.get(gameId).fireEvent(new NextRoundEvent(gameId,
                currRound,
                getState(gameId).getRoundRules().get(currRound)));
    }

    private void sendEventWhenAllAnswered(String gameId, QuizQuestionModel currQuestion) {
        boolean noMoreQuestionsInRound = getState(gameId).prepareNextQuestionAndCheckIsLast();
        boolean roundsOver = false;
        int currRound = getState(gameId).getRoundNumber();
        if (noMoreQuestionsInRound) {
            roundsOver = getState(gameId).prepareNextRoundAndCheckIsLast();
        }
        eventBuses.get(gameId).fireEvent(new AllUsersAnsweredEvent(gameId,
                currQuestion,
                noMoreQuestionsInRound,
                roundsOver,
                currRound));
    }

    // GameFinishedEvent
    public void sendFinishGameEvent(String gameId) {
        getState(gameId).calculateUsersStatistic();
        getState(gameId).updateUserPositions();
        eventBuses.get(gameId).fireEvent(new CleverestBroadcastService.GameFinishedEvent(gameId));
    }

    // NextQuestionEvent
    public void sendNextQuestionEvent(String gameId) {
        CleverestGameState gameState = getState(gameId);
        gameState.getUsers().values().forEach(CleverestGameState.UserGameState::prepareForNext);

        QuizQuestionModel question = null;
//        if (gameState.specialQuestionShouldAppear()) {
//            question = gameState.getSpecial();
//            if (question == null) {
//                System.out.println("No more special. Send base question");
//                question = gameState.getCurrent();
//            }
//        } else {
//            question = gameState.getCurrent();
//        }
        question = gameState.getCurrent();
        eventBuses.get(gameId).fireEvent(new NextQuestionEvent(gameId, question, gameState.getRoundNumber()));
    }

    // UpdatePersonalScoreEvent
    public void sendUpdatePersonalScoreEvent(String gameId) {
        eventBuses.get(gameId).fireEvent(new UpdatePersonalScoreEvent(gameId));
    }

    // RenderCategoriesEvent
    public void sendRenderCategoriesEvent(String gameId, String category, QuizQuestionModel question, boolean initial) {
        CleverestGameState gameState = getState(gameId);
        if (initial) {
            gameState.prepareUsersForThirdRound();
        }
        gameState.getUsers().values().forEach(CleverestGameState.UserGameState::prepareForNext);
        if (category != null && question != null) {
            gameState.getThirdQuestions().get(category).remove(question);
        }
        Set<String> finishedCategories = gameState.getThirdQuestions().entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        finishedCategories.forEach(key -> gameState.getThirdQuestions().remove(key));
        if (gameState.getThirdQuestions().isEmpty()) {
            sendFinishGameEvent(gameId);
            return;
        }
        eventBuses.get(gameId).fireEvent(new RenderCategoriesEvent(gameId,
                gameState.getUsersToAnswer().next(),
                gameState.getThirdQuestions()));
    }

    public static class CleverestGameEvent extends ComponentEvent<Div> {
        @Getter
        private String gameId;

        public CleverestGameEvent(String gameId) {
            super(new Div(), false);
        }
    }

    @Getter
    public static class UserJoinedEvent extends CleverestGameEvent {
        @Getter
        private String username;

        public UserJoinedEvent(String gameId,
                               String username) {
            super(gameId);
            this.username = username;
        }
    }

    @Getter
    public static class UserBetEvent extends CleverestGameEvent {
        @Getter
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
        private String username;

        public UserAnsweredEvent(String gameId, String username) {
            super(gameId);
            this.username = username;
        }
    }

    @Getter
    public static class AllUsersAnsweredEvent extends CleverestGameEvent {
        private QuizQuestionModel question;
        private boolean roundOver;
        private boolean roundsOver;
        private int currentRoundNumber;

        public AllUsersAnsweredEvent(String gameId, QuizQuestionModel question, boolean roundOver, boolean roundsOver, int currentRoundNumber) {
            super(gameId);
            this.question = question;
            this.roundOver = roundOver;
            this.roundsOver = roundsOver;
            this.currentRoundNumber = currentRoundNumber;
        }
    }

    @Getter
    public static class NextRoundEvent extends CleverestGameEvent {
        private int roundNumber;
        private String rules;

        public NextRoundEvent(String gameId, int roundNumber, String rules) {
            super(gameId);
            this.roundNumber = roundNumber;
            this.rules = rules;
        }
    }

    @Getter
    public static class NextQuestionEvent extends CleverestGameEvent {
        private QuizQuestionModel question;
        private int roundNumber;

        public NextQuestionEvent(String gameId, QuizQuestionModel question, int roundNumber) {
            super(gameId);
            this.question = question;
            this.roundNumber = roundNumber;
        }
    }

    @Getter
    public static class RenderCategoriesEvent extends CleverestGameEvent {
        private CleverestGameState.UserGameState userToAnswer;
        private Map<String, List<QuizQuestionModel>> data;

        public RenderCategoriesEvent(String gameId,
                                     CleverestGameState.UserGameState userToAnswer,
                                     Map<String, List<QuizQuestionModel>> data) {
            super(gameId);
            this.userToAnswer = userToAnswer;
            this.data = data;
        }
    }

    @Getter
    public static class SaveUserAnswersEvent extends CleverestGameEvent {
        private QuizQuestionModel question;
        private List<CleverestGameState.UserGameState> userStates;

        public SaveUserAnswersEvent(String gameId,
                                    QuizQuestionModel question,
                                    List<CleverestGameState.UserGameState> userStates) {
            super(gameId);
            this.question = question;
            this.userStates = userStates;
        }
    }

    @Getter
    public static class GameFinishedEvent extends CleverestGameEvent {
        public GameFinishedEvent(String gameId) {
            super(gameId);
        }
    }

    public <T extends ComponentEvent<?>> Registration subscribe(String gameId,
                                                                Class<T> eventType,
                                                                ComponentEventListener<T> listener) {
        ComponentEventBus eventBus = eventBuses.computeIfAbsent(gameId, bus -> new ComponentEventBus(new Div()));
        return eventBus.addListener(eventType, listener);
    }
}
