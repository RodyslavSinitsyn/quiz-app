package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.rsinitsyn.quiz.model.sound.GameSound;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.*;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;
import static org.rsinitsyn.quiz.utils.AudioUtils.playStaticSoundAsync;
import static org.rsinitsyn.quiz.utils.QuizComponents.appendTextBorder;
import static org.rsinitsyn.quiz.utils.QuizUtils.*;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;
import static org.rsinitsyn.quiz.utils.StaticValuesHolder.*;

@Slf4j
public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private String gameId;
    private CleverestBroadcaster broadcaster;
    private boolean gameHost;
    private Runnable hostAction = null;

    private final Div topContainer = new Div();
    private final Div midContainer = new Div();
    private final CleverestResultComponent resultComponent = new CleverestResultComponent();

    private final List<Registration> subscriptions = new ArrayList<>();

    /**
     * Entry point. Called from GamePage.configureAndAddPlayBoardComponent().
     * UI must be passed explicitly — never rely on getUI() here because
     * setState() is called before the component is attached (UI = -1 at that moment).
     * Subscriptions are registered here (not in onAttach) so they are created
     * exactly once per setState call, regardless of how many times Vaadin
     * triggers attach/detach during the same navigation cycle.
     */
    public void setState(String gameId,
                         CleverestBroadcaster broadcaster,
                         boolean gameHost,
                         boolean refreshEvent,
                         UI ui) {
        logState(this, ui, "SetState", true, subscriptions);
        // Always clear first — guards against double-attach during @PreserveOnRefresh refresh cycle
        clearSubs();

        this.gameId = gameId;
        this.broadcaster = broadcaster;
        this.gameHost = gameHost;

        // Register all subscriptions right here with the known-good UI reference
        subscribeOnEvents(ui);

        if (gameHost) {
            int currRound = broadcaster.getState(gameId).getRoundNumber();
            if (refreshEvent) {
                renderCurrentQuestion();
            } else {
                showRoundRules(currRound, broadcaster.getState(gameId).getRoundRules().get(currRound));
            }
        } else {
            renderUserPersonalScore();
            if (refreshEvent) {
                restoreCurrentQuestion();
            }
        }
        topContainer.setWidthFull();
        midContainer.setWidthFull();
        add(topContainer, midContainer);
        logState(this, ui, "SetState", false, subscriptions);
    }

    /**
     * Restores the current question from broadcaster state during a page refresh.
     * Does NOT call getCurrentQuestion() to avoid the side-effect of resetting
     * questionRenderedTime — reads the question list directly by index instead.
     * For round 3 (categories), renders the categories table instead of a question.
     */
    private void restoreCurrentQuestion() {
        CleverestGameState state = broadcaster.getState(gameId);
        final var roundNumber = state.getRoundNumber();
        if (roundNumber == 3) {
            // Round 3 is categories-based — nothing to restore here,
            // the RenderCategoriesEvent will re-render on next action.
            // Just show a waiting message for players.
            if (!gameHost) {
                midContainer.add(userInfoLightSpan("В ожидании вопроса", LumoUtility.TextColor.SECONDARY, CleverestComponents.MOBILE_LARGE_FONT));
            }
            return;
        }
        final var userRefreshState = state.getUserRefreshState(getLoggedUser());
        renderQuestionLayout(userRefreshState.question(),
                userRefreshState.questionNumber(),
                userRefreshState.totalQuestionsSize(),
                roundNumber);
        midContainer.setEnabled(!userRefreshState.answerGiven());
    }

    // -------------------------------------------------------------------------
    // Subscription setup — called once from setState, never from onAttach
    // -------------------------------------------------------------------------

    private void subscribeOnEvents(UI ui) {
        subscribeOnCommonEvents(ui);
        if (gameHost) {
            subscribeOnHostOnlyEvents(ui);
        } else {
            subscribeOnPlayerOnlyEvents(ui);
        }
    }

    private void subscribeOnCommonEvents(final UI ui) {
        subscriptions.add(broadcaster.subscribe(gameId, UserAnsweredEvent.class, event ->
                runActionInUi(ui, () -> {
                    if (gameHost) {
                        updateUserAnswerGiven(event.username(), event.lastResponseTimeSec());
                    }
                    if (doneByAuthenticated(event)) {
                        midContainer.setEnabled(false);
                    }
                    if (event.roundNumber() == 3) {
                        runHostAction();
                    } else {
                        notification("%s ответил!".formatted(event.username()), NotificationVariant.LUMO_PRIMARY);
                    }
                })));
        subscriptions.add(broadcaster.subscribe(gameId, GetQuestionEvent.class, event ->
                runActionInUi(ui, () -> {
                            // workaround
                            final var userState = broadcaster.getState(gameId).getUserState(getLoggedUser());
                            if (!gameHost && userState.isAnswerGiven()) {
                                return;
                            }
                            renderQuestion(
                                    event.getQuestion(),
                                    event.getQuestionNumber(),
                                    event.getTotalQuestionsInRound(),
                                    event.getRoundNumber());
                        }
                )));

        subscriptions.add(broadcaster.subscribe(gameId, RenderCategoriesEvent.class, event ->
                runActionInUi(ui, () -> {
                    midContainer.removeAll();
                    if (gameHost) {
                        renderTopContainerForHost(List.of(event.getUser().profile()));
                        renderCategoriesTable(event.getUser(), event.getData());
                    } else {
                        if (doneByAuthenticated(event)) {
                            midContainer.add(userInfoLightSpan("Время отвечать!", LumoUtility.TextColor.PRIMARY, CleverestComponents.MOBILE_LARGE_FONT));
                        } else {
                            midContainer.add(userInfoLightSpan("В ожидании вопроса", LumoUtility.TextColor.SECONDARY, CleverestComponents.MOBILE_LARGE_FONT));
                        }
                    }
                })));

        subscriptions.add(broadcaster.subscribe(gameId, RoundInfoEvent.class, event ->
                runActionInUi(ui, () -> showRoundRules(event.getRoundNumber(), event.getRules()))));

        subscriptions.add(broadcaster.subscribe(gameId, GameFinishedEvent.class, event ->
                runActionInUi(ui, this::renderResults)));
    }

    private void subscribeOnPlayerOnlyEvents(UI ui) {
        log.debug("Subscribed on player events: {}", getLoggedUser());

        subscriptions.add(broadcaster.subscribe(gameId, UpdatePersonalScoreEvent.class, event ->
                runActionInUi(ui, () -> renderUserPersonalScore())));

        subscriptions.add(broadcaster.subscribe(gameId, QuestionChoosenEvent.class, event ->
                runActionInUi(ui, () -> {
                    if (getLoggedUser().equals(event.getUsername())) {
                        renderQuestionLayout(event.getQuestion(), 1, 1, 1); // TODO Real numbers
                    }
                })));
    }

    private void subscribeOnHostOnlyEvents(UI ui) {
        log.debug("Subscribed on host events: {}", getLoggedUser());

        subscriptions.add(broadcaster.subscribe(gameId, AllUsersAnsweredEvent.class, event ->
                playStaticSoundAsync(SUBMIT_ANSWER_SHORT_AUDIOS.next()).thenRun(() ->
                        runActionInUi(ui, () ->
                                showCorrectAnswer(
                                        event.getQuestion(),
                                        broadcaster.getState(gameId).userSnapshotsSortedByResponseTime().values(),
                                        event.isRoundOver(),
                                        event.getRevealScoreAfter(),
                                        event.getQuestion().isManualApprove(),
                                        uName -> {
                                            broadcaster.getState(gameId).getUserState(uName).increaseScoreAndMarkCorrect(1);
                                            broadcaster.sendUpdatePersonalScoreEvent(gameId);
                                        },
                                        () -> {
                                        },
                                        () -> broadcaster.sendGetQuestionEvent(gameId))))));
        subscriptions.add(broadcaster.subscribe(gameId, QuestionGradedEvent.class, event ->
                runActionInUi(ui, () -> {
                    if (gameHost) {
                        updateUserGrade(event.username(), event.getEmoji());
                    }
                })));
        subscriptions.add(broadcaster.subscribe(gameId, PlaySoundEvent.class, event ->
                playStaticSoundAsync(event.getSound().path())));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", false, subscriptions);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        logState(this, detachEvent.getUI(), "OnDetach", true, subscriptions);
        clearSubs();
        logState(this, detachEvent.getUI(), "OnDetach", false, subscriptions);
    }

    private void renderTopContainerForHost(Collection<UserProfile> userGameStates) {
        if (!gameHost) {
            return;
        }
        topContainer.removeAll();
        userGameStates.forEach(profile -> {
            final var userProfile = userProfile(profile, CleverestComponents.MOBILE_LARGE_FONT);
            userProfile.setId("top-container-user-" + profile.username());
            topContainer.add(userProfile);
        });
    }

    private void updateUserAnswerGiven(String username, String lastResponseTimeSec) {
        topContainerUserComponent(username).ifPresent(component -> {
            component.addComponentAsFirst(userCheckIcon());
            component.addComponentAsFirst(appendTextBorder(new Span(lastResponseTimeSec)));
        });
    }

    private void updateUserGrade(String username, Emoji emoji) {
        topContainerUserComponent(username).ifPresent(component ->
                component.addComponentAsFirst(emoji(emoji.value)));
    }

    private Optional<HorizontalLayout> topContainerUserComponent(String username) {
        return topContainer.getChildren()
                .filter(c -> c.getId().map(id -> id.equals("top-container-user-" + username)).orElse(false))
                .map(c -> (HorizontalLayout) c)
                .findAny();
    }

    private void showRoundRules(int roundNumber, String rulesText) {
        Div rulesComponent = new Div();
        rulesComponent.setText(rulesText);
        rulesComponent.addClassNames(MOBILE_LARGE_FONT, LumoUtility.TextAlignment.CENTER);
        openDialog(rulesComponent, "", gameHost ? this::renderCurrentQuestion : () -> {
        });
    }

    private void renderCurrentQuestion() {
        if (!gameHost) {
            return;
        }
        if (broadcaster.getState(gameId).getRoundNumber() == 3) {
            broadcaster.sendRenderCategoriesEvent(gameId, null, true);
        } else {
            broadcaster.sendGetQuestionEvent(gameId);
        }
    }

    private void renderQuestion(QuestionModel question, int questionNumber, int totalQuestions, int roundNumber) {
        log.debug("Render question. GameId {}, questionText: {}", gameId, question.getText());
        renderTopContainerForHost(broadcaster.getState(gameId).getAllUserProfiles());
        renderQuestionLayout(question, questionNumber, totalQuestions, roundNumber);
    }

    private void renderQuestionLayout(QuestionModel questionModel, int questionNumber, int totalQuestions, int roundNumber) {
        midContainer.removeAll();
        midContainer.setEnabled(true);

        midContainer.add(horizontalLayoutCenter(
                new Span("Раунд %d. Вопрос %d/%d".formatted(roundNumber, questionNumber, totalQuestions))
        ));
        midContainer.add(new Hr());

        String imageHeight = gameHost ? CleverestComponents.LARGE_IMAGE_HEIGHT : CleverestComponents.MEDIUM_IMAGE_HEIGHT;
        var questionLayout = createQuestionLayout(new QuestionLayoutRequest()
                .question(questionModel)
                .host(gameHost)
                .renderCategory(false)
                .imageHeight(imageHeight));
        questionLayout.addAnsweredListener(event -> {
            broadcaster.sendSubmitAnswerEventAndCheckScore(gameId,
                    getLoggedUser(),
                    questionModel,
                    String.join(", ", event.getAnswerGivenEvent().getAnswers()),
                    () -> event.getAnswerGivenEvent().getResult());

            QuizUtils.wait(1).thenRun(() ->
                    runActionInUi(getUI(), () -> openQuestionGradeDialog(questionModel)));
        });
        midContainer.add(questionLayout);
    }

    private void openQuestionGradeDialog(QuestionModel question) {
        final var layout = horizontalLayoutCenter();
        final var dialog = openDialog(layout, "Как тебе вопрос?", () -> {
        });
        List.of(Emoji.randomBad(), Emoji.randomGood(), Emoji.randomGreat())
                .forEach(e -> {
                    final var emoji = emoji(e.value);
                    emoji.addClickListener(event -> {
                        fireEvent(new UpdateQuestionGradeEvent(
                                question,
                                getLoggedUser(),
                                e.rating
                        ));
                        broadcaster.sendQuestionGradedEvent(gameId, question, getLoggedUser(), e);
                        dialog.close();
                    });
                    layout.add(emoji);
                });
    }

    private void renderCategoriesTable(UserGameState userToAnswer, Map<String, List<QuestionModel>> data) {
        var categoriesLayout = new VerticalLayout();
        categoriesLayout.setAlignItems(Alignment.START);
        categoriesLayout.setPadding(false);

        midContainer.removeAll();
        midContainer.add(categoriesLayout);

        data.forEach((category, questions) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD);
            row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            row.setAlignItems(Alignment.START);
            row.setMargin(true);

            var categoryName = CleverestComponents.primaryButton(category, event -> {
            });
            categoryName.addClassNames(LumoUtility.FontSize.XXXLARGE);
            categoryName.setEnabled(!questions.stream().allMatch(QuestionModel::isAlreadyAnswered));

            row.add(categoryName);
            questions.forEach(questionModel -> {
                Button openQuestionButton = createCategoryQuestionButton(userToAnswer, questionModel);
                row.add(openQuestionButton);
            });
            categoriesLayout.add(row);
        });
    }

    @SneakyThrows
    private Button createCategoryQuestionButton(UserGameState userToAnswer, QuestionModel question) {
        Button button = CleverestComponents.primaryButton(String.valueOf(question.getPoints()), event -> {
            broadcaster.sendQuestionChoosenEvent(gameId, question, userToAnswer);
            Dialog questionTextDialog = openDialog(
                    createQuestionLayout(new QuestionLayoutRequest()
                            .question(question)
                            .host(gameHost)
                            .renderCategory(false)
                            .imageHeight("25em")),
                    "Вопрос",
                    () -> {
                    }
            );
            setHostAction(() -> {
                questionTextDialog.close();
                AtomicBoolean approved = new AtomicBoolean(false);
                question.setAlreadyAnswered(true);
                // 3rd round
                showCorrectAnswer(question, List.of(userToAnswer.snapshot()), false, 0, true, uName -> {
                    userToAnswer.increaseScoreAndMarkCorrect(question.getPoints());
                    approved.set(true);
                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                }, () -> {
                    if (!approved.get()) {
                        userToAnswer.decreaseScoreAndMarkWrong(question.getPoints());
                    }
                }, () -> broadcaster.sendRenderCategoriesEvent(gameId, question, false));
            });
        });
        button.setEnabled(!question.isAlreadyAnswered());
        button.addClassNames(LumoUtility.FontSize.XXXLARGE);
        return button;
    }

    private void runHostAction() {
        if (gameHost) {
            Optional.ofNullable(hostAction).ifPresentOrElse(
                    Runnable::run,
                    () -> log.debug("No host action to run, gameId: {}", gameId));
            hostAction = null;
        }
    }

    private void setHostAction(Runnable action) {
        if (gameHost) {
            this.hostAction = action;
        }
    }

    private void renderUserPersonalScore() {
        final var userState = broadcaster.getState(gameId).getUserState(getLoggedUser());
        if (userState == null) {
            log.warn("Not joined user is accessing started Cleverest game");
            return;
        }
        topContainer.removeAll();
        final var userRow = userProfileWithScore(userState.snapshot(), MOBILE_LARGE_FONT);
        final var emojiSound = CleverestComponents.soundBar(() ->
                broadcaster.sendPlaySoundEvent(gameId, GameSound.next()));
        userRow.add(emojiSound);
        topContainer.add(userRow);
        topContainer.add(new Hr());
    }

    private void showUsersPositionsTable(boolean roundOver, int revealScoreAfter, Runnable onCloseAction) {
        var usersScoreLayout = revealScoreAfter == 0
                ? usersScoreTableLayout(broadcaster.getState(gameId).usersSortedByScore().stream()
                        .map(UserGameState::snapshot)
                        .toList(),
                broadcaster.getState(gameId).getLastAnswers())
                : new VerticalLayout(userInfoLightSpan(
                "Вопросов до таблицы результатов: " + revealScoreAfter, LumoUtility.FontSize.XXXLARGE));

        openDialog(usersScoreLayout, "Таблица результатов", () -> {
            if (roundOver) {
                broadcaster.sendNewRoundEvent(gameId);
                return;
            }
            onCloseAction.run();
        });
    }

    private void showCorrectAnswer(QuestionModel question,
                                   Collection<UserStateSnapshot> users,
                                   boolean roundOver,
                                   int revealScoreAfter,
                                   boolean approveManually,
                                   Consumer<String> approveAction,
                                   Runnable onCloseAction,
                                   Runnable usersScoreCloseAction) {
        if (users.stream().allMatch(UserStateSnapshot::correct)) {
            playStaticSoundAsync(CORRECT_ANSWER_AUDIOS.next());
        } else if (users.stream().noneMatch(UserStateSnapshot::correct) && !approveManually) {
            playStaticSoundAsync(WRONG_ANSWER_AUDIOS.next());
        } else {
            playStaticSoundAsync(REVEAL_ANSWER_AUDIOS.next());
        }
        final var answersLayout = userAnswersLayout(question, users, approveManually, approveAction);
        openDialog(answersLayout, "Ответы", () -> {
            onCloseAction.run();
            broadcaster.sendUpdatePersonalScoreEvent(gameId);
            broadcaster.sendSaveUsersAnswersEvent(gameId, question);
            showUsersPositionsTable(roundOver, revealScoreAfter, usersScoreCloseAction);
        });
    }

    private void renderResults() {
        topContainer.removeAll();
        midContainer.removeAll();
        CleverestGameState gameState = broadcaster.getState(gameId);
        if (gameHost) {
            resultComponent.setState(gameState.usersSortedByScore(), gameState.getHistory(), "");
        } else {
            renderUserPersonalScore();
            midContainer.add(userInfoLightSpan("Итоговое место: " + gameState.getUserState(getLoggedUser()).getLastPosition(), CleverestComponents.MOBILE_LARGE_FONT));
            resultComponent.setState(gameState.usersSortedByScore(), gameState.getHistory(), getLoggedUser());
        }
        midContainer.add(resultComponent);
    }

    private void clearSubs() {
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }

    public class GamePlayboardEvent extends ComponentEvent<CleverestGamePlayBoardComponent> {
        public GamePlayboardEvent() {
            super(CleverestGamePlayBoardComponent.this, true);
        }
    }

    @RequiredArgsConstructor
    @Getter
    @Accessors(fluent = true)
    public class UpdateQuestionGradeEvent extends GamePlayboardEvent {
        private final QuestionModel question;
        private final String username;
        private final int grade;
    }

    public Registration addUpdateQuestionGradeEventListener(final ComponentEventListener<UpdateQuestionGradeEvent> listener) {
        return addListener(UpdateQuestionGradeEvent.class, listener);
    }
}