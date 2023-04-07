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
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.utils.QuizUtils;
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
                            Set<QuizQuestionModel> firstRound,
                            Set<QuizQuestionModel> secondRound,
                            Set<QuizQuestionModel> thirdRound,
                            Set<QuizQuestionModel> special) {
        CleverestGameState state = new CleverestGameState();
        state.init(firstRound,
                secondRound,
                thirdRound.stream().collect(Collectors.groupingBy(QuizQuestionModel::getCategoryName)),
                special);
        state.setCreatedBy(createdBy);
        gameStateMap.put(gameId, state);
    }

    // UserJoinedEvent
    public void addUserToGame(String gameId, String username) {
        CleverestGameState gameState = gameStateMap.get(gameId);
        if (QuizUtils.getLoggedUser().equals("Аноним")
                || QuizUtils.getLoggedUser().equals(gameState.getCreatedBy())) {
            return; // TODO Кастыль не регаем в игре
        }
        CleverestGameState.UserGameState emptyUserGameState = new CleverestGameState.UserGameState();
        emptyUserGameState.setUsername(username);
        gameState.getUsers().put(username, emptyUserGameState);
        eventBus.fireEvent(new UserJoinedEvent(username));
    }

    //    AllPlayersReadyEvent
    public void allPlayersReady(String gameId) {
        eventBus.fireEvent(new AllUsersReadyEvent(gameStateMap.get(gameId).getUsers().keySet()));
    }

    public void updateHistoryBatchAndSendEvent(String gameId, QuizQuestionModel question) {
        gameStateMap.get(gameId).getUsers().keySet().forEach(username -> {
            updateHistory(gameId, question, username, false);
        });
        eventBus.fireEvent(new SaveUserAnswersEvent(
                question,
                gameStateMap.get(gameId).getHistory().get(question)));
    }

    public void updateHistory(String gameId, QuizQuestionModel question, String username, boolean sendEvent) {
        gameStateMap.get(gameId).updateHistory(question, username);
        if (sendEvent) {
            eventBus.fireEvent(new SaveUserAnswersEvent(
                    question,
                    gameStateMap.get(gameId).getHistory().get(question)));
        }
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
        eventBus.fireEvent(new NextRoundEvent(gameStateMap.get(gameId).getRoundNumber()));
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
        gameState.getUsers().values().forEach(CleverestGameState.UserGameState::prepareForNext);

        QuizQuestionModel question = null;
        if (gameState.specialQuestionShouldAppear()) {
            question = gameState.getSpecial();
            if (question == null) {
                System.out.println("No more special. Send base question");
                question = gameState.getCurrent();
            }
        } else {
            question = gameState.getCurrent();
        }
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
        eventBus.fireEvent(new RenderCategoriesEvent(gameStateMap.get(gameId).getThirdQuestions()));
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

        public NextRoundEvent(int roundNumber) {
            super(new Div(), false);
            this.roundNumber = roundNumber;
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
        private Map<String, List<QuizQuestionModel>> data;

        public RenderCategoriesEvent(Map<String, List<QuizQuestionModel>> data) {
            super(new Div(), false);
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
