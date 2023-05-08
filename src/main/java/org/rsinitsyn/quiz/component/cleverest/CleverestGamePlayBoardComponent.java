package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private String gameId;
    private CleverestBroadcaster broadcaster;
    private boolean isAdmin;

    private final Div topContainer = new Div();
    private final Div midContainer = new Div();
    private final Div botContainer = new Div();

    private final List<Registration> subs = new ArrayList<>();

    public void setProperties(String gameId, CleverestBroadcaster broadcaster, boolean isAdmin) {
        System.out.println("Init gameId: " + gameId);
        this.gameId = gameId;
        this.broadcaster = broadcaster;
        this.isAdmin = isAdmin;

        validateUserOnPageEnter();
        if (isAdmin) {
            int currRound = broadcaster.getState(gameId).getRoundNumber();
            renderRoundRules(currRound, broadcaster.getState(gameId).getRoundRules().get(currRound));
        } else {
            updateUserPersonalScore();
        }

        topContainer.setWidthFull();
        midContainer.setWidthFull();
        botContainer.setWidthFull();
        add(topContainer, midContainer, botContainer);
    }

    private void validateUserOnPageEnter() {
        boolean notInGameAndNotAdmin = !broadcaster.getState(gameId).getUsers().containsKey(SessionWrapper.getLoggedUser())
                && !isAdmin;
        if (notInGameAndNotAdmin) {
            throw new IllegalStateException("Игра уже началась, вы там не учавствуете");
        }
    }

    private void renderTopContainer() {
        if (!isAdmin) {
            return;
        }
        topContainer.removeAll();
        broadcaster.getState(gameId).getUsers().values()
                .forEach(uState -> {
                    Div userDiv = new Div();
                    userDiv.getStyle().set("color", uState.getColor());
                    userDiv.add(CleverestComponents.userNameSpan(uState.getUsername(), uState.getColor()));
                    userDiv.setId("top-container-user-" + uState.getUsername());
                    userDiv.setWidthFull();
                    userDiv.addClassNames(LumoUtility.Border.BOTTOM,
                            LumoUtility.FontSize.XXLARGE);
                    topContainer.add(userDiv);
                });
    }

    private void updateUserAnswerGiven(String username) {
        var component = (Div) topContainer.getChildren()
                .filter(c -> c.getId().orElseThrow().equals("top-container-user-" + username))
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Cant update topContainer. Username not found: " + username));
        component.addComponentAsFirst(CleverestComponents.userCheckIcon());
    }

    private void renderRoundRules(int roundNumber, String rulesText) {
        Div rules = new Div();
        rules.setText(rulesText);
        rules.addClassNames(LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.TextAlignment.CENTER);
        rules.setWidth("25em");
        CleverestComponents.openDialog(
                rules,
                "Раунд " + roundNumber,
                () -> {
                    if (roundNumber == 3) {
                        broadcaster.sendRenderCategoriesEvent(gameId, null, null, true);
                    } else {
                        broadcaster.sendNextQuestionEvent(gameId);
                    }
                }
        );
    }

    private void renderQuestion(QuestionModel question,
                                int questionNumber,
                                int totalQuestions,
                                int roundNumber) {
        setEnabled(true);
        renderTopContainer();
        renderQuestionLayout(question, questionNumber, totalQuestions, roundNumber);
        boolean singleAnswer = roundNumber == 2;
        renderOptionsOrInput(question, singleAnswer);
    }

    private void renderOptionsOrInput(QuestionModel questionModel,
                                      boolean singleAnswer) {
        var answersLayout = new VerticalLayout();
        answersLayout.setPadding(false);

        botContainer.removeAll();
        botContainer.add(answersLayout);

        if (!singleAnswer) {
            questionModel.getShuffledAnswers().forEach(answerModel -> {
                Button button = CleverestComponents.optionButton(answerModel.getText(), () -> {
                    if (isAdmin) {
                        return;
                    }
                    broadcaster.sendSubmitAnswerEventAndIncreaseScore(gameId, SessionWrapper.getLoggedUser(), questionModel, answerModel);
                });
                answersLayout.add(button);
            });
        } else {
            if (isAdmin) {
                return;
            }
            Button submit = CleverestComponents.primaryButton("Ответить", e -> {
            });
            submit.setWidthFull();

            TextField textField = CleverestComponents.answerInput(event -> {
                submit.setEnabled(!event.getValue().isBlank());
            });
            submit.setEnabled(false);
            submit.addClickListener(event -> {
                broadcaster.sendSubmitAnswerEvent(gameId, SessionWrapper.getLoggedUser(), questionModel, textField.getValue());
            });
            answersLayout.add(textField, submit);
        }
    }

    private void renderQuestionLayout(QuestionModel questionModel, int questionNumber, int totalQuestions, int roundNumber) {
        List<String> questionClasses = isAdmin
                ? List.of(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD)
                : List.of(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.LIGHT);

        String imageHeight = isAdmin ? "25em" : "15em";

        midContainer.removeAll();

        Span questionNumberTooltip = new Span();
        questionNumberTooltip.setText(String.format("Раунд %d. Вопрос %d/%d", roundNumber, questionNumber, totalQuestions));
        questionNumberTooltip.addClassNames(LumoUtility.FontSize.SMALL,
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.AlignSelf.START);

        midContainer.add(questionNumberTooltip);
        midContainer.add(CleverestComponents.questionLayout(
                questionModel,
                questionClasses,
                imageHeight,
                isAdmin
        ));
    }

    private void renderCategoriesTable(UserGameState userToAnswer, Map<String, List<QuestionModel>> data) {
        var categoriesLayout = new VerticalLayout();
        categoriesLayout.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.LIGHT);
        categoriesLayout.setAlignItems(Alignment.CENTER);
        categoriesLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        midContainer.removeAll();
        midContainer.add(categoriesLayout);

        categoriesLayout.add(new Span(new Span("Отвечает: "),
                CleverestComponents.userNameSpan(
                        userToAnswer.getUsername(),
                        userToAnswer.getColor())));

        data.forEach((category, questions) -> {
            Button openQuestionBtn = CleverestComponents.primaryButton(
                    category + " (" + questions.size() + ")",
                    event -> {
                        QuestionModel question = questions.stream().findFirst().orElseThrow();
                        // TODO KASTIL
                        broadcaster.getState(gameId).refreshQuestionRenderedTime();
                        broadcaster.getState(gameId).submitAnswer(userToAnswer.getUsername(), "");

                        VerticalLayout questionLayout = CleverestComponents.questionLayout(question,
                                List.of(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD),
                                "20em",
                                isAdmin
                        );
                        CleverestComponents.openDialog(
                                questionLayout,
                                "Вопрос",
                                () -> {
                                    showCorrectAnswer(question,
                                            Collections.singletonList(userToAnswer),
                                            false,
                                            0,
                                            true,
                                            uName -> {
                                                broadcaster.getState(gameId).getUsers().get(uName).increaseScore();
                                                broadcaster.sendUpdatePersonalScoreEvent(gameId);
                                            },
                                            () -> broadcaster.sendRenderCategoriesEvent(gameId, category, question, false));
                                });
                    });
            openQuestionBtn.setWidthFull();
            openQuestionBtn.addClassNames(LumoUtility.FontSize.XXXLARGE);
            categoriesLayout.add(openQuestionBtn);
        });
    }


    private void updateUserPersonalScore() {
        UserGameState userState = broadcaster.getState(gameId).getUsers().get(SessionWrapper.getLoggedUser());
        topContainer.removeAll();
        topContainer.add(CleverestComponents.userScoreLayout(
                SessionWrapper.getLoggedUser(),
                userState.getColor(),
                userState.getScore(),
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.LIGHT
        ));
    }

    private void showUsersScore(QuestionModel question,
                                boolean roundOver,
                                int revealScoreAfter,
                                Runnable onCloseAction) {
        var usersScoreLayout = revealScoreAfter == 0
                ? CleverestComponents.usersScoreTableLayout(broadcaster.getState(gameId).getSortedByScoreUsers())
                : new VerticalLayout(new Span("Таблица результатов через " + revealScoreAfter + " количество вопросов..."));

        CleverestComponents.openDialog(
                usersScoreLayout,
                "Таблица результатов",
                () -> {
                    broadcaster.sendUpdateHistoryEvent(gameId, question);
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
                                   Runnable onCloseAction) {
        if (users.stream().allMatch(UserGameState::isLastWasCorrect)) {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.CORRECT_ANSWER_AUDIOS.next());
        } else if (users.stream().noneMatch(UserGameState::isLastWasCorrect) && !approveManually) {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.WRONG_ANSWER_AUDIOS.next());
        } else {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
        }
        VerticalLayout answers = new VerticalLayout();
        answers.setSpacing(true);
        answers.setDefaultHorizontalComponentAlignment(Alignment.START);
        answers.setAlignItems(Alignment.CENTER);
        answers.addClassNames(LumoUtility.FontSize.XXXLARGE);

        answers.add(CleverestComponents.correctAnswerSpan(question,
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.SEMIBOLD));
        answers.add(new Hr());

        users.forEach(userGameState -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setDefaultVerticalComponentAlignment(Alignment.CENTER);

            if (!approveManually) {
                row.add(userGameState.isLastWasCorrect()
                        ? CleverestComponents.doneIcon()
                        : CleverestComponents.cancelIcon());
            }

            Span userAnswer = CleverestComponents
                    .userAnswerSpan(userGameState.getUsername(),
                            userGameState.getColor(),
                            userGameState.getLastAnswerText(),
                            userGameState.isLastWasCorrect(),
                            LumoUtility.FontSize.XXXLARGE);
            row.add(userAnswer);
            if (approveManually) {
                Button approveButton = CleverestComponents.approveButton(
                        () -> approveAction.accept(userGameState.getUsername()),
                        ButtonVariant.LUMO_SUCCESS);
                row.add(approveButton);
            }
            answers.add(row);
            answers.add(new Hr());
        });

        CleverestComponents.openDialog(
                answers,
                "Ответы",
                () -> {
                    broadcaster.getState(gameId).updateUserPositions();
                    showUsersScore(question, roundOver, revealScoreAfter, onCloseAction);
                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                });
    }

    private void renderResults() {
        topContainer.removeAll();
        midContainer.removeAll();
        botContainer.removeAll();
        if (isAdmin) {
            midContainer.add(new CleverestResultComponent(
                    broadcaster.getState(gameId).getSortedByScoreUsers().values(),
                    broadcaster.getState(gameId).getHistory()));
        } else {
            updateUserPersonalScore();
            midContainer.add(new Span("Итоговое место: "
                    + broadcaster.getState(gameId).getUsers().get(SessionWrapper.getLoggedUser()).getLastPosition()));
            botContainer.add(new CleverestResultComponent(
                    broadcaster.getState(gameId).getSortedByScoreUsers().values(),
                    broadcaster.getState(gameId).getHistory(),
                    SessionWrapper.getLoggedUser()));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UserAnsweredEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                CleverestComponents.notification(event.getUsername() + " ответил",
                        NotificationVariant.LUMO_CONTRAST);
                if (isAdmin) {
                    updateUserAnswerGiven(event.getUsername());
                }
                if (event.getUsername().equals(SessionWrapper.getLoggedUser())) {
                    setEnabled(false);
                }
            });
        }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.NextQuestionEvent.class,
                event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(),
                            () -> renderQuestion(event.getQuestion(),
                                    event.getQuestionNumber(),
                                    event.getTotalQuestionsInRound(),
                                    event.getRoundNumber()));
                }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.RenderCategoriesEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                midContainer.removeAll();
                botContainer.removeAll();
                if (isAdmin) {
                    renderCategoriesTable(event.getUserToAnswer(), event.getData());
                } else {
                    if (SessionWrapper.getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
                        midContainer.add(CleverestComponents.userWaitSpan(
                                "Время отвечать!",
                                LumoUtility.TextColor.PRIMARY
                        ));
                    } else {
                        midContainer.add(CleverestComponents.userWaitSpan(
                                "Внимание на экран!",
                                LumoUtility.TextColor.SECONDARY
                        ));
                    }
                }
            });
        }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GameFinishedEvent.class,
                event -> QuizUtils.runActionInUi(attachEvent.getUI().getUI(), this::renderResults)));

        if (isAdmin) {
            subscribeOnAdminOnlyEvents(attachEvent);
        } else {
            subscribeOnPlayerOnlyEvents(attachEvent);
        }
    }

    private void subscribeOnPlayerOnlyEvents(AttachEvent attachEvent) {
        subs.add(broadcaster.subscribe(gameId,
                CleverestBroadcaster.UpdatePersonalScoreEvent.class,
                event -> QuizUtils.runActionInUi(attachEvent.getUI().getUI(), this::updateUserPersonalScore)));
    }

    private void subscribeOnAdminOnlyEvents(AttachEvent attachEvent) {
        subs.add(broadcaster.subscribe(gameId,
                CleverestBroadcaster.AllUsersAnsweredEvent.class,
                event -> {
                    AudioUtils.playStaticSoundAsync(StaticValuesHolder.SUBMIT_ANSWER_SHORT_AUDIOS.next())
                            .thenRun(() -> {
                                QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                                    boolean approveManually = event.getCurrentRound() == 2;
                                    showCorrectAnswer(
                                            event.getQuestion(),
                                            broadcaster.getState(gameId).getSortedByResponseTimeUsers().values(),
                                            event.isRoundOver(),
                                            event.getRevealScoreAfter(),
                                            approveManually,
                                            uName -> {
                                                broadcaster.getState(gameId).getUsers().get(uName).increaseScore();
                                                broadcaster.sendUpdatePersonalScoreEvent(gameId);
                                            },
                                            () -> broadcaster.sendNextQuestionEvent(gameId));
                                });
                            });
                }));

        subs.add(broadcaster.subscribe(gameId,
                CleverestBroadcaster.SaveUserAnswersEvent.class,
                this::fireEvent));

        subs.add(broadcaster.subscribe(gameId,
                CleverestBroadcaster.NextRoundEvent.class,
                event -> QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                    renderRoundRules(event.getRoundNumber(), event.getRules());
                })));
    }

    public <T extends ComponentEvent<?>> Registration subscribe(Class<T> eventType,
                                                                ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subs.forEach(Registration::remove);
    }
}