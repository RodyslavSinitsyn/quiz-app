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
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.component.custom.event.UserEvent;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.answer.AnswerResult;
import org.rsinitsyn.quiz.model.cleverest.*;
import org.rsinitsyn.quiz.model.sound.GameSound;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
        log.info("Create game state: [{}]", gameId);
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
                                  String color,
                                  String photoUrl,
                                  String winnerBet,
                                  String loserBet) {
        CleverestGameState gameState = getState(gameId);
        final var userState = gameState.addOrUpdateUser(gameId, username, color, photoUrl, winnerBet, loserBet);
        eventBuses.get(gameId).fireEvent(new UserJoinedEvent(gameId, userState.profile(), gameState.getAllUserProfiles()));
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

        state.updateUserPositions();
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
                                                   Supplier<AnswerResult> answerResultProvider) {
        log.info("User gave answer: {} = {}", username, answerAsText);
        final var state = getState(gameId);
        final var userGameState = getState(gameId).getUserState(username);
        userGameState.submitAnswer(answerAsText, state.getQuestionRenderedTime(), answerResultProvider);
        eventBuses.get(gameId).fireEvent(
                new UserAnsweredEvent(gameId,
                        userGameState.getUsername(),
                        userGameState.getLastResponseTimeSec(),
                        getState(gameId).getRoundNumber()));

        sendEventIfAllAnswered(gameId, questionModel);
    }

    public void sendNextRoundEvent(String gameId) {
        final var state = getState(gameId);
        final var gameOver = state.prepareNextRoundAndCheckIsGameOver();
        if (gameOver) {
            sendFinishGameEvent(gameId);
            return;
        }
        final var roundNumber = state.getRoundNumber();
        log.info("Send next round {}", roundNumber);
        eventBuses.get(gameId).fireEvent(new RoundInfoEvent(gameId, roundNumber, state.getRoundRules().get(roundNumber)));
    }

    private void sendEventIfAllAnswered(String gameId, QuestionModel currQuestion) {
        if (getState(gameId).areAllUsersAnswered()
                && getState(gameId).getRoundNumber() != 3) {
            final var state = getState(gameId);
            boolean roundOver = state.lastQuestionInRound();
            log.info("All users answered. CurrQuestionNumber: {}. NextQuestionNumber: {}. TotalQuestons: {}, CurrRound: {}. RoundIsOver: {}.",
                    state.getQuestionNumber() - 1,
                    state.getQuestionNumber(),
                    state.getCurrRoundQuestionsSource().get().size(),
                    state.getRoundNumber(),
                    roundOver);
            eventBuses.get(gameId).fireEvent(new AllUsersAnsweredEvent(gameId,
                    currQuestion,
                    roundOver,
                    state.getRoundNumber(),
                    state.getCountToRevealScoreTable()));
        }
    }

    // GameFinishedEvent
    public void sendFinishGameEvent(String gameId) {
        getState(gameId).calculateUsersStatistic();
        getState(gameId).updateUserPositions();
        log.info("Game finished: {}", gameId);
        eventBuses.get(gameId).fireEvent(new CleverestBroadcaster.GameFinishedEvent(gameId));
    }

    // GetQuestionEvent
    public void sendCurrentQuestionEvent(String gameId) {
        final var state = getState(gameId);
        final var question = state.getCurrentQuestion();

        if (question == null) {
            log.debug("Get question returns null, perhaps round just empty, render next");
            sendNextRoundEvent(gameId);
            return;
        }

        log.info("Sending current question for render: #{} {}", state.getQuestionNumber(), question.getText());
        eventBuses.get(gameId).fireEvent(new GetQuestionEvent(
                gameId,
                question,
                state.getQuestionNumber() + 1,
                state.getCurrRoundQuestionsSource().get().size(),
                state.getRoundNumber()));
    }

    public void sendNextQuestionEvent(String gameId) {
        final var state = getState(gameId);
        state.increaseQuestionNumber();
        final var question = state.getCurrentQuestion();
        log.info("Sending next question for render: #{} {}", state.getQuestionNumber(), question.getText());
        eventBuses.get(gameId).fireEvent(new GetQuestionEvent(
                gameId,
                question,
                state.getQuestionNumber() + 1,
                state.getCurrRoundQuestionsSource().get().size(),
                state.getRoundNumber()
        ));
    }

    // UpdatePersonalScoreEvent
    public void sendUpdatePersonalScoreEvent(String gameId) {
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
                                        Emoji emoji) {
        eventBuses.get(gameId)
                .fireEvent(new QuestionGradedEvent(
                        gameId,
                        questionModel,
                        username,
                        emoji.rating,
                        emoji
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

    public void sendPlaySoundEvent(String gameId, GameSound sound) {
        eventBuses.get(gameId).fireEvent(new PlaySoundEvent(gameId, sound));
    }

    public void sendDeleteUserEvent(String gameId, String username) {
        final var state = getState(gameId);
        eventBuses.get(gameId).fireEvent(new DeleteUserEvent(gameId, state.removeUser(username)));
        sendEventIfAllAnswered(gameId, state.getCurrentQuestion());
    }

    public void sendUserTextedEvent(String gameId, String username, String messageText) {
        final var state = getState(gameId);
        state.putToMessages(username, messageText);
        eventBuses.get(gameId).fireEvent(new UserSentMessageEvent(
                gameId,
                messageText,
                state.userMessagesDesc(),
                Optional.ofNullable(state.getUserState(username)).map(UserGameState::profile)));
    }

    public void sendLiveReactionEvent(String gameId, final String user, final Emoji emoji) {
        final var state = getState(gameId);
        eventBuses.get(gameId).fireEvent(
                new LiveReactionEvent(gameId, state.getUserState(user).profile(), emoji));
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
    public static class UserJoinedEvent extends CleverestGameEvent implements UserEvent {
        private final UserProfile user;
        private final List<UserProfile> allUsers;

        public UserJoinedEvent(String gameId,
                               UserProfile user,
                               List<UserProfile> allUsers) {
            super(gameId);
            this.user = user;
            this.allUsers = allUsers;
        }

        @Override
        public String username() {
            return user.username();
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class UserBetEvent extends CleverestGameEvent {
        private final UserGameState user;
        private final String userToBet;
        private final List<UserGameState> allUsers;

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
    @Accessors(fluent = true)
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
        private final int currentRound;
        private final int revealScoreAfter;

        public AllUsersAnsweredEvent(String gameId,
                                     QuestionModel question,
                                     boolean roundOver,
                                     int currentRound,
                                     int revealScoreAfter) {
            super(gameId);
            this.question = question;
            this.roundOver = roundOver;
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
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class RenderCategoriesEvent extends CleverestGameEvent implements UserEvent {
        private final UserGameState user;
        private final Map<String, List<QuestionModel>> data;

        public RenderCategoriesEvent(String gameId,
                                     UserGameState user,
                                     Map<String, List<QuestionModel>> data) {
            super(gameId);
            this.user = user;
            this.data = data;
        }

        @Override
        public String username() {
            return user.getUsername();
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
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class QuestionGradedEvent extends CleverestGameEvent implements UserEvent {
        private final QuestionModel question;
        private final String username;
        private final int grade;
        private final Emoji emoji;

        public QuestionGradedEvent(String gameId, QuestionModel question,
                                   String username, int grade, final Emoji emoji) {
            super(gameId);
            this.question = question;
            this.username = username;
            this.grade = grade;
            this.emoji = emoji;
        }

        @Override
        public String username() {
            return username;
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

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class PlaySoundEvent extends CleverestGameEvent {
        private final GameSound sound;

        public PlaySoundEvent(final String gameId, final GameSound sound) {
            super(gameId);
            this.sound = sound;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class DeleteUserEvent extends CleverestGameEvent {

        private final UserGameState deleted;

        public DeleteUserEvent(final String gameId, final UserGameState deleted) {
            super(gameId);
            this.deleted = deleted;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class UserSentMessageEvent extends CleverestGameEvent {
        private final String message;
        private final List<UserMessage> messages;
        private final Optional<UserProfile> userProfile;

        public UserSentMessageEvent(final String gameId,
                                    final String message,
                                    final List<UserMessage> messages,
                                    final Optional<UserProfile> userProfile) {
            super(gameId);
            this.message = message;
            this.messages = messages;
            this.userProfile = userProfile;
        }
    }

    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class LiveReactionEvent extends CleverestGameEvent {
        private final UserProfile userProfile;
        private final Emoji emoji;

        public LiveReactionEvent(final String gameId, final UserProfile userProfile, final Emoji emoji) {
            super(gameId);
            this.userProfile = userProfile;
            this.emoji = emoji;
        }
    }

    public <T extends CleverestGameEvent> Registration subscribe(String gameId,
                                                                 Class<T> eventType,
                                                                 ComponentEventListener<T> listener) {
        final var eventBus = eventBuses.computeIfAbsent(gameId, bus -> new ComponentEventBus(new Div()));
        Registration[] regHolder = new Registration[1];
        regHolder[0] = eventBus.addListener(eventType, event -> {
            log.info("[DEEP][Broadcaster={}] Execute [{}] reg=[{}], User [{}], UI [{}], EventBus size[{}]",
                    this.hashCode(), eventType.getSimpleName(),
                    regHolder[0].hashCode(),
                    getLoggedUser(),
                    event.getSource().getUI().map(Objects::hashCode).orElse(-1),
                    eventBuses.get(gameId).getListeners(CleverestGameEvent.class).size());
            listener.onComponentEvent(event);
        });
        Registration reg = regHolder[0];
        log.info("[DEEP][Broadcaster={}] Registered [{}], User [{}], Registration [{}], EventBus size[{}]",
                this.hashCode(), eventType.getSimpleName(), getLoggedUser(), reg.hashCode(), eventBuses.get(gameId).getListeners(CleverestGameEvent.class).size());
        return reg;
    }

    void registerEventBus(String gameId, ComponentEventBus bus) {
        eventBuses.put(gameId, bus);
    }
}
