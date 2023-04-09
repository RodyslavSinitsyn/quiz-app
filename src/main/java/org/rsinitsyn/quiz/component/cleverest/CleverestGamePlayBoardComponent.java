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
import com.vaadin.flow.component.notification.Notification;
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
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private String gameId;
    private CleverestBroadcastService broadcastService;
    private boolean isAdmin;

    private Div personalScoreContainer = new Div();
    private VerticalLayout categoriesLayout = new VerticalLayout();
    private VerticalLayout usersScoreLayout = new VerticalLayout();
    private Div questionContainer = new Div();
    private VerticalLayout answersLayout = new VerticalLayout();

    private List<Registration> subs = new ArrayList<>();

    public void setProperties(String gameId, CleverestBroadcastService broadcastService, boolean isAdmin) {
        System.out.println("Init gameId: " + gameId);
        this.gameId = gameId;
        this.broadcastService = broadcastService;
        this.isAdmin = isAdmin;

        if (!isAdmin) {
            updateUserPersonalScore();
            add(personalScoreContainer);
        } else {
            int currRound = broadcastService.getState(gameId).getRoundNumber();
            renderRoundRules(currRound, broadcastService.getState(gameId).getRoundRules().get(currRound));
            add(categoriesLayout);
        }

        add(questionContainer, answersLayout);
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
                        broadcastService.sendRenderCategoriesEvent(gameId, null, null);
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
        answersLayout.removeAll();
        answersLayout.setPadding(false);

        if (questionModel.isSpecial()) {
            return;
        }

        if (!singleAnswer) {
            questionModel.getShuffledAnswers().forEach(answerModel -> {
                Button button = CleverestComponents.optionButton(answerModel.getText(), () -> {
                    if (isAdmin) {
                        return;
                    }
                    broadcastService.submitAnswerAndIncrease(gameId, QuizUtils.getLoggedUser(), questionModel, answerModel);
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
                broadcastService.submitAnswer(gameId, QuizUtils.getLoggedUser(), questionModel, textField.getValue());
            });
            answersLayout.add(textField, submit);
        }
    }

    private void renderQuestionLayout(QuizQuestionModel questionModel, int roundNumber) {
        questionContainer.removeAll();
        questionContainer.setWidthFull();

        List<String> questionClasses = isAdmin
                ? List.of(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD)
                : List.of(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.LIGHT);

        String imageHeight = isAdmin ? "25em" : "15em";

        questionContainer.add(CleverestComponents.questionLayout(
                questionModel,
                questionClasses,
                imageHeight
        ));
    }

    private void renderCategoriesTable(CleverestGameState.UserGameState userToAnswer, Map<String, List<QuizQuestionModel>> data) {
        categoriesLayout.removeAll();
        categoriesLayout.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.LIGHT);
        categoriesLayout.setAlignItems(Alignment.CENTER);
        categoriesLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

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
                                                broadcastService.sendUpdatePersonalScoreEvent();
                                            },
                                            () -> broadcastService.sendRenderCategoriesEvent(gameId, category, question));
                                });
                    });
            openQuestionBtn.setWidthFull();
            openQuestionBtn.addClassNames(LumoUtility.FontSize.XXXLARGE);
            categoriesLayout.add(openQuestionBtn);
        });
    }

    private void updateUserPersonalScore() {
        personalScoreContainer.removeAll();
        personalScoreContainer.setWidthFull();
        personalScoreContainer.add(CleverestComponents.userScore(
                QuizUtils.getLoggedUser(),
                broadcastService.getState(gameId).getUsers().get(QuizUtils.getLoggedUser()).getColor(),
                broadcastService.getState(gameId).getUsers().get(QuizUtils.getLoggedUser()).getScore(),
                LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.LIGHT
        ));
    }

    private void showUsersScore(QuizQuestionModel question, boolean roundOver, Runnable onCloseAction) {
        usersScoreLayout.removeAll();
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
                positionSpan.setText(String.valueOf(userGameState.getLastPosition() + "."));
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
                    broadcastService.updateHistoryAndSendEvent(gameId, question);
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
        AudioUtils.playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
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
                    broadcastService.sendUpdatePersonalScoreEvent();
                });
    }

    private void renderResults() {
        removeAll();
        add(new CleverestResultComponent(
                broadcastService.getState(gameId).getUsers(),
                broadcastService.getState(gameId).getHistory()));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        subs.add(broadcastService.subscribe(CleverestBroadcastService.UserAnsweredEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                Notification notification = Notification.show(event.getUsername(), 1_000, Notification.Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);

                if (event.getUsername().equals(QuizUtils.getLoggedUser())) {
                    setEnabled(false);
                }
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.AllUsersAnsweredEvent.class, event -> {
            if (!isAdmin) {
                return;
            }
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
                            broadcastService.sendUpdatePersonalScoreEvent();
                        },
                        () -> broadcastService.sendNextQuestionEvent(gameId));
            });
//                    });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.NextQuestionEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                renderQuestion(event.getQuestion(), event.getRoundNumber());
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.RenderCategoriesEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                questionContainer.removeAll();
                answersLayout.removeAll();
                if (isAdmin) {
                    System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                    renderCategoriesTable(event.getUserToAnswer(), event.getData());
                } else {
                    if (QuizUtils.getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
                        questionContainer.add(CleverestComponents.userWaitSpan(
                                "Время отвечать!",
                                LumoUtility.TextColor.PRIMARY
                        ));
                    } else {
                        questionContainer.add(CleverestComponents.userWaitSpan(
                                "Внимание на экран!",
                                LumoUtility.TextColor.SECONDARY
                        ));
                    }
                }
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.NextRoundEvent.class, event -> {
            if (!isAdmin) {
                return;
            }
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                renderRoundRules(event.getRoundNumber(), event.getRules());
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.SaveUserAnswersEvent.class, event -> {
            if (isAdmin) {
                fireEvent(event);
            }
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.UpdatePersonalScoreEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                if (!isAdmin) {
                    updateUserPersonalScore();
                }
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.GameFinishedEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                renderResults();
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
}