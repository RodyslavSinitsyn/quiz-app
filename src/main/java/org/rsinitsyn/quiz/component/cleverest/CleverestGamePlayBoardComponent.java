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
import org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestResultComponent;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.*;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.emoji;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;
import static org.rsinitsyn.quiz.utils.AudioUtils.playStaticSoundAsync;
import static org.rsinitsyn.quiz.utils.QuizComponents.appendTextBorder;
import static org.rsinitsyn.quiz.utils.QuizUtils.doneByAuthenticated;
import static org.rsinitsyn.quiz.utils.QuizUtils.runActionInUi;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

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
        logState("SetState", true);
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
                // Restore question in midContainer first, then show the "refresh" dialog on top.
                // When host closes the dialog the question is already visible behind it.
                restoreCurrentQuestion(currRound);
                showRoundRules(currRound, "Рефреш страницы.");
            } else {
                showRoundRules(currRound, broadcaster.getState(gameId).getRoundRules().get(currRound));
            }
        } else {
            renderUserPersonalScore();
            if (refreshEvent) {
                // Re-render the current question so the player doesn't see a blank screen.
                restoreCurrentQuestion(broadcaster.getState(gameId).getRoundNumber());
            }
        }
        topContainer.setWidthFull();
        midContainer.setWidthFull();
        add(topContainer, midContainer);
        logState("SetState", false);
    }

    /**
     * Restores the current question from broadcaster state during a page refresh.
     * Does NOT call getCurrentQuestion() to avoid the side-effect of resetting
     * questionRenderedTime — reads the question list directly by index instead.
     * For round 3 (categories), renders the categories table instead of a question.
     */
    private void restoreCurrentQuestion(int roundNumber) {
        CleverestGameState state = broadcaster.getState(gameId);
        if (roundNumber == 3) {
            // Round 3 is categories-based — nothing to restore here,
            // the RenderCategoriesEvent will re-render on next action.
            // Just show a waiting message for players.
            if (!gameHost) {
                midContainer.add(userInfoLightSpan("В ожидании вопроса", LumoUtility.TextColor.SECONDARY, CleverestComponents.MOBILE_LARGE_FONT));
            }
            return;
        }
        List<QuestionModel> questions = state.getCurrRoundQuestionsSource().get();
        int idx = state.getQuestionNumber();
        if (idx >= questions.size()) {
            log.warn("restoreCurrentQuestion: questionNumber={} out of bounds (size={}), skipping", idx, questions.size());
            return;
        }
        // Read directly — no side effects on questionRenderedTime
        QuestionModel question = questions.get(idx);
        log.info("Restoring question on refresh: round={}, idx={}, question={}", roundNumber, idx, question.getText());
        if (gameHost) {
            renderTopContainerForHost(state.getAllUserProfiles());
        }
        renderQuestionLayout(question, idx + 1, questions.size(), roundNumber);
    }

    // -------------------------------------------------------------------------
    // Subscription setup — called once from setState, never from onAttach
    // -------------------------------------------------------------------------

    private void subscribeOnEvents(UI ui) {
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
                        notification(event.username() + " ответил", NotificationVariant.LUMO_CONTRAST);
                    }
                })));

        subscriptions.add(broadcaster.subscribe(gameId, GetQuestionEvent.class, event ->
                runActionInUi(ui, () ->
                        renderQuestion(
                                event.getQuestion(),
                                event.getQuestionNumber(),
                                event.getTotalQuestionsInRound(),
                                event.getRoundNumber()))));

        subscriptions.add(broadcaster.subscribe(gameId, RenderCategoriesEvent.class, event ->
                runActionInUi(ui, () -> {
                    midContainer.removeAll();
                    if (gameHost) {
                        renderTopContainerForHost(List.of(event.getUser().profile())); // todo: fix 3rd round
                        renderCategoriesTable(event.getUser(), event.getData());
                    } else {
                        if (getLoggedUser().equals(event.getUser().getUsername())) {
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

        if (gameHost) {
            subscribeOnHostOnlyEvents(ui);
        } else {
            subscribeOnPlayerOnlyEvents(ui);
        }
    }

    private void subscribeOnPlayerOnlyEvents(UI ui) {
        log.debug("Subscribed on player events: {}", getLoggedUser());

        subscriptions.add(broadcaster.subscribe(gameId, UpdatePersonalScoreEvent.class, event ->
                runActionInUi(ui, () -> {
                    log.debug("Updating score from event");
                    renderUserPersonalScore();
                })));

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
                playStaticSoundAsync(StaticValuesHolder.SUBMIT_ANSWER_SHORT_AUDIOS.next()).thenRun(() -> {
                    log.debug("Submit audio finished, run action in ui: {}, {}", ui, gameId);
                    runActionInUi(ui, () -> {
                        boolean approveManually = event.getCurrentRound() == 2;  // todo instead of manual approve use logic, only approve for TOP type?
                        showCorrectAnswer(
                                event.getQuestion(),
                                broadcaster.getState(gameId).userSnapshotsSortedByResponseTime().values(),
                                event.isRoundOver(),
                                event.getRevealScoreAfter(),
                                approveManually,
                                uName -> {
                                    broadcaster.getState(gameId).getUserState(uName).increaseScore();
                                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                                },
                                () -> {
                                },
                                () -> broadcaster.sendGetQuestionEvent(gameId));
                    });
                })));
        subscriptions.add(broadcaster.subscribe(gameId, QuestionGradedEvent.class, event -> {
            runActionInUi(ui, () -> {
                if (gameHost) {
                    updateUserGrade(event.username(), event.getEmoji());
                }
            });
        }));
    }

    // -------------------------------------------------------------------------
    // onAttach / onDetach — subscriptions are NOT managed here anymore.
    // onDetach still clears subs as a safety net (e.g. if the component is
    // removed from the layout without a new setState being called).
    // -------------------------------------------------------------------------

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // Intentionally empty — subscriptions are created in setState() with an
        // explicit UI reference. Do NOT add subscriptions here.
        logState("OnAttach (no-op)", true);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        logState("OnDetach", true);
        // Safety net: if this component is detached without a subsequent setState
        // (e.g. navigating away), clean up to prevent ghost listeners.
        clearSubs();
        logState("OnDetach", false);
    }

    // -------------------------------------------------------------------------
    // Rendering helpers (unchanged from original)
    // -------------------------------------------------------------------------

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
        rulesComponent.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextAlignment.CENTER);
        rulesComponent.setWidth("25em");
        Runnable hostAction = () -> {
            if (roundNumber == 3) {
                broadcaster.sendRenderCategoriesEvent(gameId, null, true);
            } else {
                broadcaster.sendGetQuestionEvent(gameId);
            }
        };
        openDialog(rulesComponent, "Раунд " + roundNumber, gameHost ? hostAction : () -> {
        });
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
                    () -> event.getAnswerGivenEvent().isCorrect());

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
                showCorrectAnswer(question, List.of(userToAnswer.snapshot()), false, 0, true, uName -> {
                    userToAnswer.increaseScore(question.getPoints());
                    approved.set(true);
                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                }, () -> {
                    if (!approved.get()) {
                        userToAnswer.decreaseScore(question.getPoints());
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
        topContainer.add(userProfileWithScore(userState.snapshot(), CleverestComponents.MOBILE_LARGE_FONT));
        topContainer.add(new Hr());
    }

    private void showUsersScore(boolean roundOver, int revealScoreAfter, Runnable onCloseAction) {
        var usersScoreLayout = revealScoreAfter == 0
                ? usersScoreTableLayout(broadcaster.getState(gameId).usersSortedByScore())
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
            playStaticSoundAsync(StaticValuesHolder.CORRECT_ANSWER_AUDIOS.next());
        } else if (users.stream().noneMatch(UserStateSnapshot::correct) && !approveManually) {
            playStaticSoundAsync(StaticValuesHolder.WRONG_ANSWER_AUDIOS.next());
        } else {
            playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
        }
        VerticalLayout answersLayout = new VerticalLayout();
        answersLayout.setSpacing(true);
        answersLayout.setDefaultHorizontalComponentAlignment(Alignment.START);
        answersLayout.setAlignItems(Alignment.START);
        answersLayout.addClassNames(LumoUtility.FontSize.XXXLARGE);

        answersLayout.add(correctAnswerSpan(question,
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.SEMIBOLD));
        question.answerDescription().ifPresent(answerDescription ->
                answersLayout.add(answerDescriptionSpan(answerDescription,
                        LumoUtility.FontSize.XXLARGE,
                        LumoUtility.FontWeight.LIGHT)));

        users.forEach(userGameState -> {
            final var userProfileWithAnswer = userProfileWithAnswer(userGameState,
                    question.getType(),
                    LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD);
            if (userGameState.correct()) {
                userProfileWithAnswer.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY);
            }
            if (!approveManually) {
                userProfileWithAnswer.add(userGameState.correct() ? doneIcon() : cancelIcon());
            }
            if (approveManually) {
                int countLimit;
                switch (question.getType()) {
                    case TOP -> countLimit = question.getAnswers().size();
                    case LINK -> countLimit = question.getAnswers().size() / 2;
                    default -> countLimit = 0;
                }
                Button approveButton = approveButton(
                        () -> approveAction.accept(userGameState.username()),
                        countLimit);
                userProfileWithAnswer.add(approveButton);
            }
            answersLayout.add(userProfileWithAnswer);
        });

        openDialog(answersLayout, "Ответы", () -> {
            onCloseAction.run();
            broadcaster.getState(gameId).updateUserPositions();
            broadcaster.sendUpdatePersonalScoreEvent(gameId);
            broadcaster.sendSaveUsersAnswersEvent(gameId, question);
            showUsersScore(roundOver, revealScoreAfter, usersScoreCloseAction);
        });
    }

    private void renderResults() {
        topContainer.removeAll();
        midContainer.removeAll();
        CleverestGameState gameState = broadcaster.getState(gameId);
        if (gameHost) {
            resultComponent.setState(gameState.usersSortedByScore().values(), gameState.getHistory(), "");
        } else {
            renderUserPersonalScore();
            midContainer.add(userInfoLightSpan("Итоговое место: " + gameState.getUserState(getLoggedUser()).getLastPosition(), CleverestComponents.MOBILE_LARGE_FONT));
            resultComponent.setState(gameState.usersSortedByScore().values(), gameState.getHistory(), getLoggedUser());
        }
        midContainer.add(resultComponent);
    }

    private void clearSubs() {
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }

    private void logState(String action, boolean start) {
        log.info("[FIX][PlayBoardComponent={}] {} [{}], User [{}], UI [{}], Subs size=[{}], items[{}]",
                this.hashCode(), start ? "Start" : "End", action, getLoggedUser(), getUI().map(Object::hashCode).orElse(-1), subscriptions.size(), subscriptions);
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