package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.UserAnsweredEvent;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.cancelIcon;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.correctAnswerSpan;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.doneIcon;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.horizontalLayoutBetween;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.userAnswerSpan;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.userScoreLayout;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.usersScoreTableLayout;
import static org.rsinitsyn.quiz.component.custom.question.BaseQuestionLayout.QuestionAnsweredEvent;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;
import static org.rsinitsyn.quiz.utils.AudioUtils.playStaticSoundAsync;
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

    public void setState(String gameId, CleverestBroadcaster broadcaster, boolean gameHost, boolean refreshEvent) {
        log.debug("Render Cleverest game with id [{}]", gameId);
        this.gameId = gameId;
        this.broadcaster = broadcaster;
        this.gameHost = gameHost;

        if (gameHost) {
            int currRound = broadcaster.getState(gameId).getRoundNumber();
            if (refreshEvent) {
                showRoundRules(currRound, "Рефреш страницы.");
            } else {
                showRoundRules(currRound, broadcaster.getState(gameId).getRoundRules().get(currRound));
            }
        } else {
            renderUserPersonalScore();
        }
        topContainer.setWidthFull();
        midContainer.setWidthFull();
        add(topContainer, midContainer);
    }

    private void renderTopContainerForHost(Collection<UserGameState> userGameStates) {
        if (!gameHost) {
            return;
        }
        topContainer.removeAll();
        userGameStates.forEach(uState -> {
            Div userDiv = new Div();
            userDiv.getStyle().set("color", uState.getColor());
            userDiv.add(CleverestComponents.userNameSpan(uState.getUsername(), uState.getColor()));
            userDiv.setId("top-container-user-" + uState.getUsername());
            userDiv.setWidthFull();
            userDiv.addClassNames(LumoUtility.Border.BOTTOM, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.XXLARGE);
            topContainer.add(userDiv);
        });
    }

    private void updateUserAnswerGiven(UserGameState userGameState) {
        var component = (Div) topContainer.getChildren()
                .filter(c -> c.getId().orElseThrow().equals("top-container-user-" + userGameState.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cant update topContainer. Username not found: " + userGameState.getUsername()));
        component.addComponentAsFirst(CleverestComponents.userCheckIcon());
        component.addComponentAsFirst(QuizComponents.appendTextBorder(new Span(userGameState.lastResponseTimeSec())));
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
        Runnable userAction = () -> {
        };
        openDialog(rulesComponent, "Раунд " + roundNumber, gameHost ? hostAction : userAction); // Show rules for all
    }

    private void renderQuestion(QuestionModel question, int questionNumber, int totalQuestions, int roundNumber) {
        log.debug("Render question. GameId {}, questionText: {}", gameId, question.getText());
        renderTopContainerForHost(broadcaster.getState(gameId).getUsers().values());
        renderQuestionLayout(question, questionNumber, totalQuestions, roundNumber);
    }

    private void renderQuestionLayout(QuestionModel questionModel, int questionNumber, int totalQuestions, int roundNumber) {
        midContainer.removeAll();
        midContainer.setEnabled(true);

        Span questionNumberSpan = new Span();
        questionNumberSpan.setText(String.format("Раунд %d. Вопрос %d/%d", roundNumber, questionNumber, totalQuestions));
        questionNumberSpan.addClassNames(
                LumoUtility.FontSize.MEDIUM,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.Margin.Bottom.MEDIUM,
                LumoUtility.AlignSelf.START);
        if (!gameHost) {
            // TODO: Grade feature temporary not working
//            midContainer.add(createQuestionGrade(questionModel));
        }
        midContainer.add(questionNumberSpan);

        List<String> questionClasses = gameHost ? List.of(LumoUtility.FontSize.XXXLARGE) : List.of(CleverestComponents.MOBILE_LARGE_FONT);
        String imageHeight = gameHost ? CleverestComponents.LARGE_IMAGE_HEIGHT : CleverestComponents.MEDIUM_IMAGE_HEIGHT;
        var questionLayout = createQuestionLayout(new QuestionLayoutRequest()
                .question(questionModel)
                .host(gameHost)
                .imageHeight(imageHeight)
                .textClasses(questionClasses));
        questionLayout.addListener(QuestionAnsweredEvent.class, event -> {
            broadcaster.sendSubmitAnswerEventAndCheckScore(gameId,
                    getLoggedUser(),
                    questionModel,
                    String.join(", ", event.getAnswerChosenEvent().getAnswers()),
                    () -> event.getAnswerChosenEvent().isCorrect());
        });
        midContainer.add(questionLayout);
    }

    private VerticalLayout createQuestionGrade(QuestionModel questionModel) {
        return questionGradeLayout(scoreVal -> {
            broadcaster.sendQuestionGradedEvent(gameId, questionModel, getLoggedUser(), scoreVal);
            notification(getLoggedUser() + ", спасибо за фидбек!", NotificationVariant.LUMO_CONTRAST);
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
                            .imageHeight("25em")
                            .textClasses(List.of(LumoUtility.FontSize.XXXLARGE))),
                    "Вопрос",
                    () -> {
                    }
            );
            setHostAction(() -> {
                questionTextDialog.close();
                AtomicBoolean approved = new AtomicBoolean(false);
                question.setAlreadyAnswered(true);
                showCorrectAnswer(question, Collections.singletonList(userToAnswer), false, 0, true, uName -> {
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
            Optional.ofNullable(hostAction).ifPresentOrElse(Runnable::run, () -> log.debug("No host action to run, gameId: {}", gameId));
            hostAction = null;
        }
    }

    private void setHostAction(Runnable action) {
        if (gameHost) {
            this.hostAction = action;
        }
    }

    private void renderUserPersonalScore() {
        UserGameState userState = broadcaster.getState(gameId).getUsers().get(getLoggedUser());
        if (userState == null) {
            // This should probably never happen
            log.warn("Not joined user is accessing started Cleverest game");
            return;
        }
        topContainer.removeAll();
        topContainer.add(userScoreLayout(getLoggedUser(), userState.getColor(), userState.getScore(), CleverestComponents.MOBILE_LARGE_FONT));
    }

    private void showUsersScore(boolean roundOver, int revealScoreAfter, Runnable onCloseAction) {
        var usersScoreLayout = revealScoreAfter == 0
                ? usersScoreTableLayout(broadcaster.getState(gameId).getSortedByScoreUsers())
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
                                   Collection<UserGameState> users,
                                   boolean roundOver,
                                   int revealScoreAfter,
                                   boolean approveManually,
                                   Consumer<String> approveAction,
                                   Runnable onCloseAction,
                                   Runnable usersScoreCloseAction) {
        if (users.stream().allMatch(UserGameState::isLastWasCorrect)) {
            playStaticSoundAsync(StaticValuesHolder.CORRECT_ANSWER_AUDIOS.next());
        } else if (users.stream().noneMatch(UserGameState::isLastWasCorrect) && !approveManually) {
            playStaticSoundAsync(StaticValuesHolder.WRONG_ANSWER_AUDIOS.next());
        } else {
            playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
        }
        VerticalLayout answersLayout = new VerticalLayout();
        answersLayout.setSpacing(true);
        answersLayout.setDefaultHorizontalComponentAlignment(Alignment.START);
        answersLayout.setAlignItems(Alignment.START);
        answersLayout.addClassNames(LumoUtility.FontSize.XXXLARGE);

        // answer span text
        answersLayout.add(correctAnswerSpan(question,
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.SEMIBOLD));
        question.answerDescription().ifPresent(answerDescription ->
                answersLayout.add(answerDescriptionSpan(answerDescription,
                        LumoUtility.FontSize.XXLARGE,
                        LumoUtility.FontWeight.LIGHT)));

        users.forEach(userGameState -> {
            final var row = horizontalLayoutBetween();
            row.setDefaultVerticalComponentAlignment(Alignment.START);
            if (userGameState.isLastWasCorrect()) {
                row.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY);
            }
            if (!approveManually) {
                row.add(userGameState.isLastWasCorrect() ? doneIcon() : cancelIcon());
            }
            Span userAnswerSpan = userAnswerSpan(userGameState,
                    question.getType(),
                    LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD);
            row.add(userAnswerSpan);
            if (approveManually) {
                int countLimit;
                switch (question.getType()) {
                    case TOP -> countLimit = question.getAnswers().size();
                    case LINK -> countLimit = question.getAnswers().size() / 2;
                    default -> countLimit = 0;
                }
                Button approveButton = approveButton(
                                () -> approveAction.accept(userGameState.getUsername()),
                                countLimit);
                row.add(approveButton);
            }
            answersLayout.add(row);
        });

        openDialog(answersLayout, "Ответы", () -> {
            onCloseAction.run();
            broadcaster.getState(gameId).updateUserPositions();
            broadcaster.sendUpdatePersonalScoreEvent(gameId);
            broadcaster.sendSaveUserAnswersEvent(gameId, question);
            showUsersScore(roundOver, revealScoreAfter, usersScoreCloseAction);
        });
    }

    private void renderResults() {
        topContainer.removeAll();
        midContainer.removeAll();
        CleverestGameState gameState = broadcaster.getState(gameId);
        if (gameHost) {
            resultComponent.setState(gameState.getSortedByScoreUsers().values(), gameState.getHistory(), "");
        } else {
            renderUserPersonalScore();
            midContainer.add(userInfoLightSpan("Итоговое место: " + gameState.getUsers().get(getLoggedUser()).getLastPosition(), CleverestComponents.MOBILE_LARGE_FONT));
            resultComponent.setState(gameState.getSortedByScoreUsers().values(), gameState.getHistory(), getLoggedUser());
        }
        midContainer.add(resultComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        log.debug("onAttach: {}", gameId);
        subscriptions.add(broadcaster.subscribe(gameId, UserAnsweredEvent.class, event -> {
            runActionInUi(attachEvent.getUI(), () -> {
                if (gameHost) {
                    updateUserAnswerGiven(event.getUserGameState());
                }
                if (event.getUserGameState().getUsername().equals(getLoggedUser())) {
                    midContainer.setEnabled(false);
                }
                if (event.getRoundNumber() == 3) {
                    runHostAction();
                } else {
                    notification(event.getUserGameState().getUsername() + " ответил", NotificationVariant.LUMO_CONTRAST);
                }
            });
        }));

        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GetQuestionEvent.class, event -> {
            runActionInUi(attachEvent.getUI(),
                    () -> renderQuestion(
                            event.getQuestion(),
                            event.getQuestionNumber(),
                            event.getTotalQuestionsInRound(),
                            event.getRoundNumber()));
        }));

        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.RenderCategoriesEvent.class, event -> {
            runActionInUi(attachEvent.getUI(), () -> {
                midContainer.removeAll();

                if (gameHost) {
                    renderTopContainerForHost(Collections.singletonList(event.getUserToAnswer()));
                    renderCategoriesTable(event.getUserToAnswer(), event.getData());
                } else {
                    if (getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
                        midContainer.add(userInfoLightSpan("Время отвечать!", LumoUtility.TextColor.PRIMARY, CleverestComponents.MOBILE_LARGE_FONT));
                    } else {
                        midContainer.add(userInfoLightSpan("В ожидании вопроса", LumoUtility.TextColor.SECONDARY, CleverestComponents.MOBILE_LARGE_FONT));
                    }
                }
            });
        }));
        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GetRoundEvent.class,
                event -> runActionInUi(attachEvent.getUI(), () -> showRoundRules(event.getRoundNumber(), event.getRules()))));
        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GameFinishedEvent.class,
                event -> runActionInUi(attachEvent.getUI(), this::renderResults)));
        if (gameHost) {
            subscribeOnHostOnlyEvents(attachEvent);
        } else {
            subscribeOnPlayerOnlyEvents(attachEvent);
        }
        log.trace("onAttach. subscribe {}", subscriptions.size());
    }

    private void subscribeOnPlayerOnlyEvents(AttachEvent attachEvent) {
        log.debug("Subscribed on player events: {}", getLoggedUser());
        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UpdatePersonalScoreEvent.class,
                event -> runActionInUi(attachEvent.getUI().getUI(), () -> {
                    log.debug("Updating score from event");
                    renderUserPersonalScore();
                })));
        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.QuestionChoosenEvent.class,
                event -> runActionInUi(attachEvent.getUI().getUI(),
                        () -> {
                            if (getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
                                runActionInUi(attachEvent.getUI().getUI(),
                                        () -> renderQuestionLayout(event.getQuestion(), 1,1,1)); // TODO Real numbers
                            }
                        })));
    }

    private void subscribeOnHostOnlyEvents(AttachEvent attachEvent) {
        log.debug("Subscribed on host events: {}", getLoggedUser());
        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.AllUsersAnsweredEvent.class, event -> {
            playStaticSoundAsync(StaticValuesHolder.SUBMIT_ANSWER_SHORT_AUDIOS.next()).thenRun(() -> {
                log.debug("Submit audio finished, run action in ui: {}, {}", attachEvent.getUI(), gameId);
                runActionInUi(attachEvent.getUI(), () -> {
                    boolean approveManually = event.getCurrentRound() == 2;
                    showCorrectAnswer(event.getQuestion(),
                            broadcaster.getState(gameId).getSortedByResponseTimeUsers().values(),
                            event.isRoundOver(),
                            event.getRevealScoreAfter(),
                            approveManually,
                            uName -> {
                                broadcaster.getState(gameId).getUsers().get(uName).increaseScore();
                                broadcaster.sendUpdatePersonalScoreEvent(gameId);
                            }, () -> {
                            }, () -> broadcaster.sendGetQuestionEvent(gameId));
                });
            });
        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.debug("onDetach: {}", gameId);
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }
}