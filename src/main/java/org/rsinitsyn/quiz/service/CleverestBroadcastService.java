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

    private Map<String, CleverestGameState> gameStateMap = new ConcurrentHashMap<>();
    private final ComponentEventBus eventBus = new ComponentEventBus(new Div());

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
    public void addUserToGame(String gameId,
                              String username,
                              String userColor,
                              String winnerBet,
                              String loserBet) {
        CleverestGameState gameState = gameStateMap.get(gameId);

        gameState.getUsers().computeIfAbsent(username, s -> {
            var state = new CleverestGameState.UserGameState();
            state.setUsername(username);
            state.setColor(userColor);
            return state;
        });

        gameState.getUsers().computeIfPresent(username, (key, userGameState) -> {
            userGameState.setColor(userColor);
            userGameState.setWinnerBet(StringUtils.defaultIfEmpty(winnerBet, ""));
            userGameState.setLoserBet(StringUtils.defaultIfEmpty(loserBet, ""));
            return userGameState;
        });

        eventBus.fireEvent(new UserJoinedEvent(username));
    }


    public void addBet(String gameId, String username, String userBet, boolean winner) {
        CleverestGameState.UserGameState userGameState = gameStateMap.get(gameId).getUsers().get(username);
        if (winner) {
            userGameState.setWinnerBet(userBet);
        } else {
            userGameState.setLoserBet(userBet);
        }
        eventBus.fireEvent(new UserBetEvent(username, userBet));
    }

    //    AllPlayersReadyEvent
    public void allPlayersReady(String gameId) {
        eventBus.fireEvent(new AllUsersReadyEvent(gameStateMap.get(gameId).getUsers().keySet()));
    }

    public void updateHistoryAndSendEvent(String gameId, QuizQuestionModel question) {
        gameStateMap.get(gameId).getUsers().entrySet()
                .stream()
                .filter(e -> e.getValue().isAnswerGiven())
                .forEach(entry -> gameStateMap.get(gameId).updateHistory(question, entry.getValue()));
        eventBus.fireEvent(new SaveUserAnswersEvent(
                question,
                gameStateMap.get(gameId).getHistory().get(question)));
    }


    // SubmitUserAnswerEvent && AllUsersAnsweredEvent
    public void submitAnswerAndIncrease(String gameId, String username, QuizQuestionModel questionModel, QuizQuestionModel.QuizAnswerModel answer) {
        gameStateMap.get(gameId).submitAnswerAndIncrease(username, answer);
        eventBus.fireEvent(new UserAnsweredEvent(username));

        if (gameStateMap.get(gameId).areAllUsersAnswered()) {
            sendEventWhenAllAnswered(gameId, questionModel);
        }
    }

    // SubmitUserAnswerEvent && AllUsersAnsweredEvent
    public void submitAnswer(String gameId, String username, QuizQuestionModel questionModel, String textAnswer) {
        gameStateMap.get(gameId).submitAnswer(username, textAnswer);
        eventBus.fireEvent(new UserAnsweredEvent(username));

        if (gameStateMap.get(gameId).areAllUsersAnswered()) {
            sendEventWhenAllAnswered(gameId, questionModel);
        }
    }

    public void sendNewRoundEvent(String gameId) {
        int currRound = gameStateMap.get(gameId).getRoundNumber();
        eventBus.fireEvent(new NextRoundEvent(
                currRound,
                gameStateMap.get(gameId).getRoundRules().get(currRound)));
    }

    private void sendEventWhenAllAnswered(String gameId, QuizQuestionModel currQuestion) {
        boolean noMoreQuestionsInRound = gameStateMap.get(gameId).prepareNextQuestionAndCheckIsLast();
        boolean roundsOver = false;
        int currRound = gameStateMap.get(gameId).getRoundNumber();
        if (noMoreQuestionsInRound) {
            roundsOver = gameStateMap.get(gameId).prepareNextRoundAndCheckIsLast();
        }
        eventBus.fireEvent(new AllUsersAnsweredEvent(
                currQuestion,
                noMoreQuestionsInRound,
                roundsOver,
                currRound));
    }

    // GameFinishedEvent
    public void finishGame() {
        eventBus.fireEvent(new CleverestBroadcastService.GameFinishedEvent());
    }

    // NextQuestionEvent
    public void sendNextQuestionEvent(String gameId) {
        CleverestGameState gameState = gameStateMap.get(gameId);
        // TODO WHEN RELOAD PAGE 1ST ROUND SYSTEM DROPS ANSWERGIVEN
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
        eventBus.fireEvent(new NextQuestionEvent(question, gameState.getRoundNumber()));
    }

    // UpdatePersonalScoreEvent
    public void sendUpdatePersonalScoreEvent() {
        eventBus.fireEvent(new UpdatePersonalScoreEvent());
    }

    public void sendRenderCategoriesEvent(String gameId, String category, QuizQuestionModel question) {
        CleverestGameState gameState = gameStateMap.get(gameId);
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
            eventBus.fireEvent(new GameFinishedEvent());
            return;
        }
        eventBus.fireEvent(new RenderCategoriesEvent(
                gameState.getUsersToAnswer().next(),
                gameState.getThirdQuestions()));
    }

    @Getter
    public static class UserJoinedEvent extends ComponentEvent<Div> {
        @Getter
        private String username;

        public UserJoinedEvent(String username) {
            super(new Div(), false);
            this.username = username;
        }
    }

    @Getter
    public static class UserBetEvent extends ComponentEvent<Div> {
        @Getter
        private String username;
        private String userToBet;

        public UserBetEvent(String username, String userToBet) {
            super(new Div(), false);
            this.username = username;
            this.userToBet = userToBet;
        }
    }

    @Getter
    public static class UpdatePersonalScoreEvent extends ComponentEvent<Div> {
        public UpdatePersonalScoreEvent() {
            super(new Div(), false);
        }
    }


    @Getter
    public static class AllUsersReadyEvent extends ComponentEvent<Div> {
        private Set<String> usernames;

        public AllUsersReadyEvent(Set<String> usernames) {
            super(new Div(), false);
            this.usernames = usernames;
        }
    }

    @Getter
    public static class UserAnsweredEvent extends ComponentEvent<Div> {
        private String username;

        public UserAnsweredEvent(String username) {
            super(new Div(), false);
            this.username = username;
        }
    }

    @Getter
    public static class AllUsersAnsweredEvent extends ComponentEvent<Div> {
        private QuizQuestionModel question;
        private boolean roundOver;
        private boolean roundsOver;
        private int currentRoundNumber;

        public AllUsersAnsweredEvent(QuizQuestionModel question, boolean roundOver, boolean roundsOver, int currentRoundNumber) {
            super(new Div(), false);
            this.question = question;
            this.roundOver = roundOver;
            this.roundsOver = roundsOver;
            this.currentRoundNumber = currentRoundNumber;
        }
    }

    @Getter
    public static class NextRoundEvent extends ComponentEvent<Div> {
        private int roundNumber;
        private String rules;

        public NextRoundEvent(int roundNumber, String rules) {
            super(new Div(), false);
            this.roundNumber = roundNumber;
            this.rules = rules;
        }
    }

    @Getter
    public static class NextQuestionEvent extends ComponentEvent<Div> {
        private QuizQuestionModel question;
        private int roundNumber;

        public NextQuestionEvent(QuizQuestionModel question, int roundNumber) {
            super(new Div(), false);
            this.question = question;
            this.roundNumber = roundNumber;
        }
    }

    @Getter
    public static class RenderCategoriesEvent extends ComponentEvent<Div> {
        private CleverestGameState.UserGameState userToAnswer;
        private Map<String, List<QuizQuestionModel>> data;

        public RenderCategoriesEvent(CleverestGameState.UserGameState userToAnswer, Map<String, List<QuizQuestionModel>> data) {
            super(new Div(), false);
            this.userToAnswer = userToAnswer;
            this.data = data;
        }
    }

    @Getter
    public static class SaveUserAnswersEvent extends ComponentEvent<Div> {
        private QuizQuestionModel question;
        private List<CleverestGameState.UserGameState> userStates;

        public SaveUserAnswersEvent(QuizQuestionModel question, List<CleverestGameState.UserGameState> userStates) {
            super(new Div(), false);
            this.question = question;
            this.userStates = userStates;
        }
    }

    @Getter
    public static class GameFinishedEvent extends ComponentEvent<Div> {
        public GameFinishedEvent() {
            super(new Div(), false);
        }
    }

    public <T extends ComponentEvent<?>> Registration subscribe(Class<T> eventType,
                                                                ComponentEventListener<T> listener) {
        return eventBus.addListener(eventType, listener);
    }
}
