package org.rsinitsyn.quiz.service;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventBus;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.shared.Registration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
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
                            Set<QuizQuestionModel> firstRound,
                            Set<QuizQuestionModel> secondRound) {
        CleverestGameState state = new CleverestGameState();
        state.init(firstRound, secondRound);
        gameStateMap.put(gameId, state);
    }

    // UserJoinedEvent
    public void addUserToGame(String gameId, String username) {
        gameStateMap.get(gameId).getUsers().put(username, new CleverestGameState.UserGameState());
        eventBus.fireEvent(new UserJoinedEvent(username));
    }

    //    AllPlayersReadyEvent
    public void allPlayersReady() {
        eventBus.fireEvent(new AllUsersReadyEvent());
    }

    // SubmitUserAnswerEvent && AllUsersAnsweredEvent
    public void submitAnswer(String gameId, String username, QuizQuestionModel.QuizAnswerModel answer) {
        gameStateMap.get(gameId).submitAnswer(username, answer);
        eventBus.fireEvent(new UserAnsweredEvent(username));

        if (gameStateMap.get(gameId).areAllUsersAnswered()) {
            sendEventWhenAllAnswered(gameId);
        }
    }

    public void submitAnswer(String gameId, String username, String textAnswer) {
        gameStateMap.get(gameId).submitAnswer(username, textAnswer);
        eventBus.fireEvent(new UserAnsweredEvent(username));

        if (gameStateMap.get(gameId).areAllUsersAnswered()) {
            sendEventWhenAllAnswered(gameId);
        }
    }

    public void sendNewRoundEvent(String gameId) {
        eventBus.fireEvent(new NextRoundEvent(gameStateMap.get(gameId).getRoundNumber()));
    }

    private void sendEventWhenAllAnswered(String gameId) {
        gameStateMap.get(gameId).getUsers().values().forEach(userGameState -> userGameState.setCurrAnswered(false));
        boolean noMoreQuestionsInRound = gameStateMap.get(gameId).prepareNextQuestionAndCheckIsLast();
        boolean lastRound = false;
        int currRound = gameStateMap.get(gameId).getRoundNumber();
        if (noMoreQuestionsInRound) {
            lastRound = gameStateMap.get(gameId).prepareNextRoundAndCheckIsLast();
        }
        eventBus.fireEvent(new AllUsersAnsweredEvent(
                noMoreQuestionsInRound,
                lastRound,
                currRound));
    }

    // GameFinishedEvent
    public void finishGame() {
        eventBus.fireEvent(new CleverestBroadcastService.GameFinishedEvent());
    }

    // NextQuestionEvent
    public void sendNextQuestionEvent() {
        eventBus.fireEvent(new NextQuestionEvent());
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
    public static class AllUsersReadyEvent extends ComponentEvent<Div> {
        public AllUsersReadyEvent() {
            super(new Div(), false);
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
        private boolean roundOver;
        private boolean gameFinished;
        private int currentRoundNumber;

        public AllUsersAnsweredEvent(boolean roundOver, boolean gameFinished, int currentRoundNumber) {
            super(new Div(), false);
            this.roundOver = roundOver;
            this.gameFinished = gameFinished;
            this.currentRoundNumber = currentRoundNumber;
        }
    }

    @Getter
    public static class NextRoundEvent extends ComponentEvent<Div> {
        private int roundNumber;

        public NextRoundEvent(int roundNumber) {
            super(new Div(), false);
            this.roundNumber = roundNumber;
        }
    }

    @Getter
    public static class NextQuestionEvent extends ComponentEvent<Div> {
        public NextQuestionEvent() {
            super(new Div(), false);
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
