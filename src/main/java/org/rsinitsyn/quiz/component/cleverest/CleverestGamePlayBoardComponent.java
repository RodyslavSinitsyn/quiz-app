package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.custom.AnimatedLeaderboardComponent;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.answer.AnswerBet;
import org.rsinitsyn.quiz.model.cleverest.*;
import org.rsinitsyn.quiz.model.sound.GameSound;
import org.rsinitsyn.quiz.model.sound.GameSounds;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.*;

import java.time.Duration;
import java.util.*;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;
import static org.rsinitsyn.quiz.utils.AudioHolder.*;
import static org.rsinitsyn.quiz.utils.AudioUtils.playStaticSoundAsync;
import static org.rsinitsyn.quiz.utils.QuizComponents.appendTextBorder;
import static org.rsinitsyn.quiz.utils.QuizComponents.openConfirmDialog;
import static org.rsinitsyn.quiz.utils.QuizUtils.*;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Slf4j
public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private String gameId;
    private CleverestBroadcaster broadcaster;
    private boolean gameHost;

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
                showRoundRules(broadcaster.getState(gameId).getRoundRules().get(currRound));
            }
        } else {
            renderUserProfile();
            if (refreshEvent) {
                restoreCurrentQuestion();
            }
        }
        topContainer.setWidthFull();
        midContainer.setWidthFull();
        add(topContainer, midContainer);
        logState(this, ui, "SetState", false, subscriptions);
    }

    private void showRoundRules(String rulesText) {
        openDialog(horizontalLayoutCenter(new Span(rulesText)), "", gameHost ? this::renderCurrentQuestion : () -> {
        });
    }

    private void renderCurrentQuestion() {
        if (broadcaster.getState(gameId).getRoundNumber() == 3) {
            broadcaster.sendRenderCategoriesEvent(gameId, null);
        } else {
            broadcaster.sendCurrentQuestionEvent(gameId);
        }
    }

    /**
     * Restores the current question from broadcaster state during a page refresh.
     * Does NOT call getCurrentQuestion() to avoid the side-effect of resetting
     * questionRenderedTime — reads the question list directly by index instead.
     * For round 3 (categories), renders the categories table instead of a question.
     */
    private void restoreCurrentQuestion() {
        final var state = broadcaster.getState(gameId);
        final var roundNumber = state.getRoundNumber();
        final var userRefreshState = state.getUserRefreshState(getLoggedUser());
        if (roundNumber == 3) {
            if (state.getThirdRoundCurrentUser().getUsername().equals(getLoggedUser())) {
                // todo: reduce copy-past
                renderQuestionLayout(new QuestionLayoutRequest()
                                .question(userRefreshState.question())
                                .host(false)
                                .username(Optional.of(getLoggedUser()))
                                .renderCategory(true)
                                .manualAnswer(true)
                                .answerBet(Optional.of(new AnswerBet(1, 5)))
                                .imageHeight(MEDIUM_IMAGE_HEIGHT),
                        new QuestionDetails(1, 1, 3));
            } else {
                midContainer.add(horizontalLayoutCenter(
                        infoSpan("В ожидании вопроса", MOBILE_LARGE_FONT)));
            }
        } else {
            renderQuestionLayout(userRefreshState.question(), userRefreshState.details());
        }
        midContainer.setEnabled(!userRefreshState.answerGiven());
    }

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
                        udpateUserTopDetails(event.username(), event.lastResponseTimeSec());
                    }
                    if (doneByAuthenticated(event)) {
                        midContainer.setEnabled(false);
                    }
                    notification("%s ответил!".formatted(event.username()),
                            NotificationVariant.LUMO_CONTRAST,
                            Notification.Position.TOP_START);
                })));
        subscriptions.add(broadcaster.subscribe(gameId, GetQuestionEvent.class, event ->
                runActionInUi(ui, () -> {
                            // workaround
                            final var userState = broadcaster.getState(gameId).getUserState(getLoggedUser());
                            if (!gameHost && userState.isAnswerGiven()) {
                                return;
                            }
                            if (gameHost) {
                                renderTopContainerForHost(broadcaster.getState(gameId).getAllUserProfiles());
                            }
                            renderQuestionLayout(event.getQuestion(), event.getDetails());
                        }
                )));

        subscriptions.add(broadcaster.subscribe(gameId, RenderCategoriesEvent.class, event ->
                runActionInUi(ui, () -> {
                    midContainer.removeAll();
                    if (gameHost) {
                        renderTopContainerForHost(List.of(event.getUser().profile()));
                        renderFlexCategoriesTable(event.getUser(), event.getQuestions());
                    } else {
                        if (doneByAuthenticated(event)) {
                            midContainer.setEnabled(true);
                            renderFlexCategoriesTable(event.getUser(), event.getQuestions());
                        } else {
                            midContainer.add(horizontalLayoutCenter(
                                    infoSpan("В ожидании вопроса", MOBILE_LARGE_FONT)));
                        }
                    }
                })));

        subscriptions.add(broadcaster.subscribe(gameId, QuestionChoosenEvent.class, event ->
                runActionInUi(ui, () -> {
                    if (gameHost) {
                        renderQuestionLayout(new QuestionLayoutRequest()
                                        .question(event.getQuestion())
                                        .host(gameHost)
                                        .renderCategory(true)
                                        .hideAnswers(true)
                                        .imageHeight(LARGE_IMAGE_HEIGHT),
                                new QuestionDetails(1, 1, 3));
                    } else if (doneByAuthenticated(event)) {
//                        playStaticSoundAsync(THINK_AUDIOS.next()); todo: same music stacks
                        renderQuestionLayout(new QuestionLayoutRequest()
                                        .question(event.getQuestion())
                                        .host(false)
                                        .username(Optional.ofNullable(event.getUsername()))
                                        .renderCategory(true)
                                        .manualAnswer(true)
                                        .answerBet(Optional.of(new AnswerBet(1, 5)))
                                        .imageHeight(MEDIUM_IMAGE_HEIGHT),
                                new QuestionDetails(1, 1, 3));
                    }
                })));

        subscriptions.add(broadcaster.subscribe(gameId, RoundInfoEvent.class, event ->
                runActionInUi(ui, () -> showRoundRules(event.getRules()))));

//        subscriptions.add(broadcaster.subscribe(gameId, GameFinishedEvent.class, event ->
//                runActionInUi(ui, this::renderResults)));

        subscriptions.add(broadcaster.subscribe(gameId, UserSentMessageEvent.class, event ->
                runActionInUi(ui, () -> chatNotification(event.getUserProfile().orElseThrow(), event.getMessage()))));
    }

    private void subscribeOnPlayerOnlyEvents(UI ui) {
        log.debug("Subscribed on player events: {}", getLoggedUser());

        subscriptions.add(broadcaster.subscribe(gameId, UpdatePersonalScoreEvent.class, event ->
                runActionInUi(ui, this::renderUserProfile)));
    }

    private void subscribeOnHostOnlyEvents(UI ui) {
        log.debug("Subscribed on host events: {}", getLoggedUser());

        subscriptions.add(broadcaster.subscribe(gameId, AllUsersAnsweredEvent.class, event ->
                        playStaticSoundAsync(SUBMIT_ANSWER_SHORT_AUDIOS.next()).thenRun(() ->
                                runActionInUi(ui, () -> {
                                    final Runnable closeAnswersAction = event.getCurrentRound() == 3
                                            ? () -> {
                                        broadcaster.getState(gameId).updateThirdRoundCurrentUser();
                                        broadcaster.sendRenderCategoriesEvent(gameId, event.getQuestion());
                                    }
                                            : () -> {
                                        if (event.isRoundOver()) {
                                            broadcaster.sendNextRoundEvent(gameId);
                                        } else {
                                            broadcaster.sendNextQuestionEvent(gameId);
                                        }
                                    };
                                    showCorrectAnswer(
                                            event.getQuestion(),
                                            event.getUserStateSnapshots(),
                                            event.getRevealScoreAfter(),
                                            manualApprove(event.getQuestion()),
                                            closeAnswersAction);
                                }))
                )
        );

        subscriptions.add(broadcaster.subscribe(gameId, QuestionGradedEvent.class, event ->
                runActionInUi(ui, () -> {
                    updateUserGrade(event.username(), event.getEmoji());
                    spawnReaction(ui, event.getEmoji(), 5);
                })));

        subscriptions.add(broadcaster.subscribe(gameId, PlaySoundEvent.class, event ->
                playStaticSoundAsync(event.getSound().path())));

        subscriptions.add(broadcaster.subscribe(gameId, DeleteUserEvent.class, event ->
                renderTopContainerForHost(broadcaster.getState(gameId).getAllUserProfiles())));

        subscriptions.add(broadcaster.subscribe(gameId, LiveReactionEvent.class, event ->
                runActionInUi(ui, () -> spawnReaction(ui, event.getEmoji()))));

        subscriptions.add(broadcaster.subscribe(gameId, UpdateUserDetailsEvent.class, event ->
                runActionInUi(ui, () -> {
                    udpateUserTopDetails(event.username(), event.updateText());
                })));
    }

    private Optional<ManualApprove> manualApprove(QuestionModel question) {
        if (!question.isManualApprove()) {
            return Optional.empty();
        }
        final var round3 = broadcaster.getState(gameId).getRoundNumber() == 3;
        final var maxPoints = round3
                ? question.getPoints()
                : switch (question.getType()) {
            case TOP -> question.getAnswers().size();
            case LINK -> question.getAnswers().size() / 2;
            case GUESS_PHOTO -> question.getHints().size();
            default -> 1;
        };
        return Optional.of(new ManualApprove(maxPoints,
                username -> {
                    broadcaster.getState(gameId).getUserState(username).increaseScoreAndMarkCorrect(1);
                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                },
                username -> {
                    broadcaster.getState(gameId).getUserState(username).decreaseScoreAndMarkWrong(1);
                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                }));
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
            final var userProfile = userProfile(profile, MOBILE_LARGE_FONT);
            userProfile.setId("top-container-user-" + profile.username());
            userProfile.addDoubleClickListener(event ->
                    openConfirmDialog(
                            new Span("Удалить игрока [%s]".formatted(profile.username())),
                            "",
                            () -> broadcaster.sendDeleteUserEvent(gameId, profile.username())));
            topContainer.add(userProfile);
        });
    }

    private void udpateUserTopDetails(String username, String updateText) {
        topContainerUserComponent(username).ifPresent(component -> {
            component.removeAll();
            component.addComponentAsFirst(userCheckIcon());
            component.addComponentAsFirst(appendTextBorder(new Span(updateText)));
        });
    }


    private void updateUserGrade(String username, Emoji emoji) {
        topContainerUserComponent(username).ifPresent(component ->
                component.addComponentAsFirst(emojiBig(emoji.value)));
    }

    private Optional<HorizontalLayout> topContainerUserComponent(String username) {
        return topContainer.getChildren()
                .filter(c -> c.getId().map(id -> id.equals("top-container-user-" + username)).orElse(false))
                .map(c -> (HorizontalLayout) c)
                .findAny();
    }

    private void renderQuestionLayout(QuestionModel questionModel, QuestionDetails details) {
        renderQuestionLayout(new QuestionLayoutRequest()
                .question(questionModel)
                .host(gameHost)
                .renderCategory(false)
                .imageHeight(gameHost ? LARGE_IMAGE_HEIGHT : MEDIUM_IMAGE_HEIGHT), details);
    }

    private void renderQuestionLayout(QuestionLayoutRequest questionLayoutRequest, QuestionDetails details) {
        final var questionModel = questionLayoutRequest.question();
        log.debug("Render question. GameId {}, questionText: {}", gameId, questionModel.getText());
        midContainer.removeAll();
        midContainer.setEnabled(true);

        midContainer.add(horizontalLayoutCenter(
                new Span("Раунд %d. Вопрос %d/%d".formatted(details.round(), details.number(), details.round()))
        ));
        midContainer.add(new Hr());

        var questionLayout = createQuestionLayout(questionLayoutRequest);
        questionLayout.addAnsweredListener(event -> {
            broadcaster.sendSubmitAnswerEventAndCheckScore(gameId,
                    getLoggedUser(),
                    questionModel,
                    String.join(", ", event.getAnswerGivenEvent().getAnswers()),
                    () -> event.getAnswerGivenEvent().getResult());

            waitAsync(1).thenRun(() ->
                    runActionInUi(getUI(), () -> openQuestionGradeDialog(questionModel)));
        });

        questionLayout.getAnswersLayout().addInputChangedListener(event ->
                broadcaster.sendUpdateUserDetailsEvent(gameId, event.username(), event.text()));

        questionLayout.getAnswersLayout().addBetChangedListener(event ->
                broadcaster.sendUpdateUserBetEvent(gameId, event.username(), event.value()));

        midContainer.add(questionLayout);
    }

    private void openQuestionGradeDialog(QuestionModel question) {
        final var layout = horizontalLayoutCenter();
        final var dialog = openDialog(layout, "Как тебе вопрос?", () -> {
        });
        List.of(Emoji.randomBad(), Emoji.randomMid(), Emoji.randomGood())
                .forEach(e -> {
                    final var emoji = emojiBig(e.value);
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

    private void renderInlineCategoriesTable(UserGameState userToAnswer, Map<String, List<QuestionModel>> data) {
        var categoriesLayout = new VerticalLayout();
        categoriesLayout.setAlignItems(Alignment.START);
        categoriesLayout.setPadding(false);

        midContainer.removeAll();
        midContainer.add(categoriesLayout);

        data.forEach((category, questions) -> {
            HorizontalLayout row = horizontalLayout(JustifyContentMode.START);
            row.setMargin(true);

            var categoryName = primaryButton(category, event -> {
            });
            categoryName.addClassNames(MOBILE_MEDIUM_FONT);
            categoryName.setEnabled(!questions.stream().allMatch(QuestionModel::isAlreadyAnswered));
            row.add(categoryName);

            questions.forEach(questionModel -> {
                Button openQuestionButton = createCategoryQuestionButton(userToAnswer, questionModel);
                row.add(openQuestionButton);
            });
            categoriesLayout.add(row);
        });
    }

    private void renderFlexCategoriesTable(UserGameState userToAnswer, List<QuestionModel> questions) {
        final var categoriesLayout = new FlexLayout();
        categoriesLayout.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        categoriesLayout.getStyle().set("gap", "10px");

        midContainer.removeAll();
        midContainer.add(categoriesLayout);

        questions.forEach((q) -> {
            final var button = createCategoryQuestionButton(userToAnswer, q);
            categoriesLayout.add(button);
        });
    }

    @SneakyThrows
    private Button createCategoryQuestionButton(UserGameState userToAnswer, QuestionModel question) {
        final var button = primaryButton(
                question.getCategoryName(),
                event -> broadcaster.sendQuestionChosenEvent(gameId, question, userToAnswer));
        button.setEnabled(!question.isAlreadyAnswered());
        button.addClassNames(MOBILE_MEDIUM_FONT);
        return button;
    }

    private void renderUserProfile() {
        final var userState = broadcaster.getState(gameId).getUserState(getLoggedUser());
        if (userState == null) {
            log.warn("Not joined user is accessing started Cleverest game");
            return;
        }
        topContainer.removeAll();

        topContainer.add(userProfileWithScore(userState.snapshot(), MOBILE_LARGE_FONT));
        topContainer.add(horizontalLayoutCenter(
                soundButton(() -> broadcaster.sendPlaySoundEvent(gameId, GameSounds.next())), // TODO: Better meme handling and chosing
                openChatButton(messageText -> broadcaster.sendUserTextedEvent(gameId, getLoggedUser(), messageText)),
                reactionButton(Emoji.HEART.value, (emoji) -> broadcaster.sendLiveReactionEvent(gameId, getLoggedUser(), emoji))));
        topContainer.add(new Hr());
    }

    private void showUsersPositionsTable(List<UserStateSnapshot> users, int revealScoreAfter, Runnable onCloseAction) {
        var usersScoreLayout = revealScoreAfter == 0
                ? usersScoreTableLayout(broadcaster.getState(gameId).usersSortedByScore(users).stream()
                        .map(UserGameState::snapshot)
                        .toList(),
                broadcaster.getState(gameId).getLastAnswers())
                : new VerticalLayout(infoSpan(
                "Вопросов до таблицы результатов: %d".formatted(revealScoreAfter), MOBILE_MEDIUM_FONT));

        // Show animated and then show normal
        if (revealScoreAfter == 0) {
            final var lastAnswersNew = broadcaster.getState(gameId).getLastAnswersNew();
            final var delay = Duration.ofMillis(3_000);
            final var animatedLeaderboard = new AnimatedLeaderboardComponent(lastAnswersNew, delay);
            final var dialog = openDialog(animatedLeaderboard, "Обновление таблицы...", () ->
                    openDialog(usersScoreLayout, "Таблица результатов", onCloseAction));
            dialog.setWidthFull();
        } else {
            openDialog(usersScoreLayout, "Таблица результатов", onCloseAction);
        }
    }

    private void showCorrectAnswer(QuestionModel question,
                                   Collection<UserStateSnapshot> users,
                                   int revealScoreAfter,
                                   Optional<ManualApprove> manualApprove,
                                   Runnable usersScoreCloseAction) {
        if (users.stream().allMatch(UserStateSnapshot::correct)) {
            playStaticSoundAsync(CORRECT_ANSWER_AUDIOS.next());
        } else if (users.stream().noneMatch(UserStateSnapshot::correct) && manualApprove.isEmpty()) {
            playStaticSoundAsync(WRONG_ANSWER_AUDIOS.next());
        } else {
            playStaticSoundAsync(REVEAL_ANSWER_AUDIOS.next());
        }
        final var answersLayout = userAnswersLayout(question, users, manualApprove);
        openDialog(answersLayout, "Ответы", () -> {
            broadcaster.sendUpdatePersonalScoreEvent(gameId);
            broadcaster.sendSaveUsersAnswersEvent(gameId, question);
            showUsersPositionsTable(new ArrayList<>(users), revealScoreAfter, usersScoreCloseAction);
        });
    }

    private void spawnReaction(final UI ui, Emoji emoji) {
        spawnReaction(ui, emoji, 3);
    }

    private void spawnReaction(final UI ui, Emoji emoji, int times) {
        for (int i = 0; i < times; i++) {
            ui.getPage().executeJs("window.spawnReaction($0)", emoji.value);
        }
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