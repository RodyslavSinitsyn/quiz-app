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
import com.vaadin.flow.component.icon.VaadinIcon;
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
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.CleverestBroadcastService;
import org.rsinitsyn.quiz.service.CleverestGameState;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private String gameId;
    private CleverestBroadcastService broadcastService;
    private boolean isAdmin;

    private final Div topContainer = new Div();
    private final Div midContainer = new Div();
    private final Div botContainer = new Div();

    private final List<Registration> subs = new ArrayList<>();

    public void setProperties(String gameId, CleverestBroadcastService broadcastService, boolean isAdmin) {
        System.out.println("Init gameId: " + gameId);
        this.gameId = gameId;
        this.broadcastService = broadcastService;
        this.isAdmin = isAdmin;

        if (isAdmin) {
            int currRound = broadcastService.getState(gameId).getRoundNumber();
            renderRoundRules(currRound, broadcastService.getState(gameId).getRoundRules().get(currRound));
        } else {
            updateUserPersonalScore();
        }

        topContainer.setWidthFull();
        midContainer.setWidthFull();
        botContainer.setWidthFull();
        add(topContainer, midContainer, botContainer);
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
                        broadcastService.sendRenderCategoriesEvent(gameId, null, null, true);
                    } else {
                        broadcastService.sendNextQuestionEvent(gameId);
                    }
                }
        );
    }

    private void renderQuestion(QuizQuestionModel question, int roundNumber) {
        setEnabled(true);
        renderQuestionLayout(question, roundNumber);
        boolean singleAnswer = roundNumber == 2 || question.isSpecial();
        renderOptions(question, singleAnswer);
    }

    private void renderOptions(QuizQuestionModel questionModel,
                               boolean singleAnswer) {
        var answersLayout = new VerticalLayout();
        answersLayout.setPadding(false);

        botContainer.removeAll();
        botContainer.add(answersLayout);

        if (questionModel.isSpecial()) {
            return;
        }

        if (!singleAnswer) {
            questionModel.getShuffledAnswers().forEach(answerModel -> {
                Button button = CleverestComponents.optionButton(answerModel.getText(), () -> {
                    if (isAdmin) {
                        return;
                    }
                    broadcastService.sendSubmitAnswerEventAndIncreaseScore(gameId, QuizUtils.getLoggedUser(), questionModel, answerModel);
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
                broadcastService.sendSubmitAnswerEvent(gameId, QuizUtils.getLoggedUser(), questionModel, textField.getValue());
            });
            answersLayout.add(textField, submit);
        }
    }

    private void renderQuestionLayout(QuizQuestionModel questionModel, int roundNumber) {
        List<String> questionClasses = isAdmin
                ? List.of(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD)
                : List.of(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.LIGHT);

        String imageHeight = isAdmin ? "25em" : "15em";

        midContainer.removeAll();
        midContainer.add(CleverestComponents.questionLayout(
                questionModel,
                questionClasses,
                imageHeight
        ));
    }

    private void renderCategoriesTable(CleverestGameState.UserGameState userToAnswer, Map<String, List<QuizQuestionModel>> data) {
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
                        QuizQuestionModel question = questions.stream().findFirst().orElseThrow();
                        // TODO KASTIL
                        broadcastService.getState(gameId).refreshQuestionRenderTime();
                        broadcastService.getState(gameId).submitAnswer(userToAnswer.getUsername(), "");

                        VerticalLayout questionLayout = CleverestComponents.questionLayout(question,
                                List.of(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD),
                                "20em"
                        );
                        CleverestComponents.openDialog(
                                questionLayout,
                                "Вопрос",
                                () -> {
                                    showCorrectAnswer(question,
                                            Collections.singletonList(userToAnswer),
                                            false,
                                            true,
                                            uName -> {
                                                broadcastService.getState(gameId).getUsers().get(uName).increaseScore();
                                                broadcastService.sendUpdatePersonalScoreEvent(gameId);
                                            },
                                            () -> broadcastService.sendRenderCategoriesEvent(gameId, category, question, false));
                                });
                    });
            openQuestionBtn.setWidthFull();
            openQuestionBtn.addClassNames(LumoUtility.FontSize.XXXLARGE);
            categoriesLayout.add(openQuestionBtn);
        });
    }


    private void updateUserPersonalScore() {
        CleverestGameState.UserGameState userState = broadcastService.getState(gameId).getUsers().get(QuizUtils.getLoggedUser());
        topContainer.removeAll();
        topContainer.add(CleverestComponents.userScore(
                QuizUtils.getLoggedUser(),
                userState.getColor(),
                userState.getScore(),
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.LIGHT
        ));
    }

    private void showUsersScore(QuizQuestionModel question, boolean roundOver, Runnable onCloseAction) {
        var usersScoreLayout = new VerticalLayout();
        Map<String, CleverestGameState.UserGameState> users =
                broadcastService.getState(gameId).getSortedByScoreUsers();
        users.forEach((username, userGameState) -> {
            HorizontalLayout row = new HorizontalLayout();

            Span positionSpan = new Span();
            positionSpan.addClassNames(LumoUtility.FontSize.XXXLARGE,
                    LumoUtility.FontWeight.SEMIBOLD,
                    LumoUtility.Border.RIGHT,
                    LumoUtility.BorderColor.CONTRAST);
            if (userGameState.getLastPosition() == 1) {
                positionSpan.add(VaadinIcon.ACADEMY_CAP.create());
            } else if (userGameState.getLastPosition() == users.size()) {
                positionSpan.add(VaadinIcon.GLASS.create());
            } else {
                positionSpan.setText(userGameState.getLastPosition() + ".");
            }
            row.add(positionSpan);

            row.add(CleverestComponents.userScore(username,
                    userGameState.getColor(),
                    userGameState.getScore(),
                    LumoUtility.FontSize.XXXLARGE));

            usersScoreLayout.add(row);
            usersScoreLayout.add(new Hr());
        });
        CleverestComponents.openDialog(
                usersScoreLayout,
                "Таблица результатов",
                () -> {
                    broadcastService.sendUpdateHistoryEvent(gameId, question);
                    if (roundOver) {
                        broadcastService.sendNewRoundEvent(gameId);
                        return;
                    }
                    onCloseAction.run();
                });
    }

    private void showCorrectAnswer(QuizQuestionModel question,
                                   Collection<CleverestGameState.UserGameState> users,
                                   boolean roundOver,
                                   boolean appendApproveButton,
                                   Consumer<String> leftApproveAction,
                                   Runnable onCloseAction) {
//        AudioUtils.playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
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

            if (!appendApproveButton) {
                row.add(userGameState.isLastWasCorrect()
                        ? CleverestComponents.doneIcon()
                        : CleverestComponents.cancelIcon());
            }

            Span userAnswer = CleverestComponents
                    .userAnswer(userGameState.getUsername(),
                            userGameState.getColor(),
                            userGameState.getLastAnswerText(),
                            userGameState.isLastWasCorrect(),
                            LumoUtility.FontSize.XXXLARGE);
            row.add(userAnswer);
            if (appendApproveButton) {
                Button approveButton = CleverestComponents.approveButton(
                        () -> leftApproveAction.accept(userGameState.getUsername()),
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
                    broadcastService.getState(gameId).updateUserPositions();
                    showUsersScore(question, roundOver, onCloseAction);
                    broadcastService.sendUpdatePersonalScoreEvent(gameId);
                });
    }

    private void renderResults() {
        topContainer.removeAll();
        midContainer.removeAll();
        botContainer.removeAll();
        if (isAdmin) {
            midContainer.add(new CleverestResultComponent(
                    broadcastService.getState(gameId).getSortedByScoreUsers().values(),
                    broadcastService.getState(gameId).getHistory()));
        } else {
            updateUserPersonalScore();
            midContainer.add(new Span("Итоговое место: "
                    + broadcastService.getState(gameId).getUsers().get(QuizUtils.getLoggedUser()).getLastPosition()));
            botContainer.add(new CleverestResultComponent(
                    broadcastService.getState(gameId).getSortedByScoreUsers().values(),
                    broadcastService.getState(gameId).getHistory(),
                    QuizUtils.getLoggedUser()));
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        subs.add(broadcastService.subscribe(gameId, CleverestBroadcastService.UserAnsweredEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                CleverestComponents.notification(event.getUsername() + " ответил",
                        NotificationVariant.LUMO_CONTRAST);
                if (event.getUsername().equals(QuizUtils.getLoggedUser())) {
                    setEnabled(false);
                }
            });
        }));

        subs.add(broadcastService.subscribe(gameId, CleverestBroadcastService.NextQuestionEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                renderQuestion(event.getQuestion(), event.getRoundNumber());
            });
        }));

        subs.add(broadcastService.subscribe(gameId, CleverestBroadcastService.RenderCategoriesEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                midContainer.removeAll();
                botContainer.removeAll();
                if (isAdmin) {
                    System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                    renderCategoriesTable(event.getUserToAnswer(), event.getData());
                } else {
                    if (QuizUtils.getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
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

        subs.add(broadcastService.subscribe(gameId, CleverestBroadcastService.GameFinishedEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                renderResults();
            });
        }));

        if (isAdmin) {
            subscribeOnAdminOnlyEvents(attachEvent);
        } else {
            subscribeOnPlayerOnlyEvents(attachEvent);
        }
    }

    private void subscribeOnPlayerOnlyEvents(AttachEvent attachEvent) {
        subs.add(broadcastService.subscribe(gameId,
                CleverestBroadcastService.UpdatePersonalScoreEvent.class,
                event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), this::updateUserPersonalScore);
                }));
    }

    private void subscribeOnAdminOnlyEvents(AttachEvent attachEvent) {
        subs.add(broadcastService.subscribe(gameId,
                CleverestBroadcastService.AllUsersAnsweredEvent.class,
                event -> {
//            AudioUtils.playStaticSoundAsync(StaticValuesHolder.SUBMIT_ANSWER_AUDIOS.next())
//                    .thenRun(() -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                        boolean approveManually = event.getCurrentRoundNumber() == 2
                                || event.getQuestion().isSpecial();
                        showCorrectAnswer(
                                event.getQuestion(),
                                broadcastService.getState(gameId).getSortedByResponseTimeUsers().values(),
                                event.isRoundOver(),
                                approveManually,
                                uName -> {
                                    broadcastService.getState(gameId).getUsers().get(uName).increaseScore();
                                    broadcastService.sendUpdatePersonalScoreEvent(gameId);
                                },
                                () -> broadcastService.sendNextQuestionEvent(gameId));
                    });
//                    });
                }));

        subs.add(broadcastService.subscribe(gameId,
                CleverestBroadcastService.SaveUserAnswersEvent.class,
                this::fireEvent));

        subs.add(broadcastService.subscribe(gameId,
                CleverestBroadcastService.NextRoundEvent.class,
                event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                        renderRoundRules(event.getRoundNumber(), event.getRules());
                    });
                }));
    }

    public <T extends ComponentEvent<?>> Registration subscribe(Class<T> eventType,
                                                                ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subs.forEach(Registration::remove);
    }


    //    private void renderTopContainer() {
//        topContainer.removeAll();
//        broadcastService.getState(gameId).getUsers().values()
//                .forEach(uState -> {
//                    Div userDiv = new Div();
//                    userDiv.getStyle().set("color", uState.getColor());
//                    userDiv.setText(uState.getUsername());
//                    userDiv.setId(uState.getUsername());
//                    userDiv.setWidthFull();
//                    userDiv.addClassNames(LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST);
//                    topContainer.add(userDiv);
//                });
//    }

//    private void appendUserAnswerSubmission(String username) {
//        if (!isAdmin) {
//            return;
//        }
//        topContainer.getChildren()
//                .filter(component -> component.getId().orElseThrow().equals(username))
//                .findFirst().orElseThrow()
//                .addClassNames(LumoUtility.Background.CONTRAST);
//    }
}