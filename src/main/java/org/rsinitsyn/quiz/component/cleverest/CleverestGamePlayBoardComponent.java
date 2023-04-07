package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.CleverestBroadcastService;
import org.rsinitsyn.quiz.service.CleverestGameState;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private final String gameId;
    private final CleverestBroadcastService broadcastService;
    private final boolean isAdmin;

    private Div personalScoreContainer = new Div();
    private VerticalLayout categoriesLayout = new VerticalLayout();
    private VerticalLayout usersScoreLayout = new VerticalLayout();
    private VerticalLayout questionLayout = new VerticalLayout();
    private VerticalLayout answersLayout = new VerticalLayout();

    private List<Registration> subs = new ArrayList<>();

    public CleverestGamePlayBoardComponent(String gameId, CleverestBroadcastService broadcastService, boolean isAdmin) {
        System.out.println("Init gameId: " + gameId);
        this.gameId = gameId;
        this.broadcastService = broadcastService;
        this.isAdmin = isAdmin;

        if (!isAdmin) {
            updateUserPersonalScore();
            add(personalScoreContainer);
        } else {
            renderRoundRules(broadcastService.getState(gameId).getRoundNumber());
            add(categoriesLayout);
        }

        add(questionLayout, answersLayout);
    }

    private void renderRoundRules(int roundNumber) {
        Span rules = new Span("Lorem Ipsum - это текст-\"рыба\", часто используемый в печати и вэб-дизайне. Lorem Ipsum является стандартной \"рыбой\" для текстов на латинице с начала XVI века. В то время некий безымянный печатник создал большую коллекцию размеров и форм шрифтов, используя Lorem Ipsum для распечатки образцов. Lorem Ipsum не только успешно пережил без заметных изменений пять веков, но и перешагнул в электронный дизайн. Его популяризации в новое время послужили публикация листов Letraset с образцами Lorem Ipsum в 60-х годах и, в более недавнее время, программы электронной вёрстки типа Aldus PageMaker, в шаблонах которых используется Lorem Ipsum.");
        rules.addClassNames(LumoUtility.FontSize.LARGE);
        Dialog rulesDialog = CleverestComponents.defaultDialog(
                rules,
                "Новый раунд",
                () -> {
                    if (roundNumber == 3) {
                        broadcastService.sendRenderCategoriesEvent(gameId, null, null);
                    } else {
                        broadcastService.sendNextQuestionEvent(gameId);
                    }
                }
        );
        rulesDialog.open();
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
            Button submit = CleverestComponents.submitButton();
            TextField textField = CleverestComponents.defaultInput(event -> {
                submit.setEnabled(!event.getValue().isBlank());
            });
            submit.addClickListener(event -> {
                broadcastService.submitAnswer(gameId, QuizUtils.getLoggedUser(), questionModel, textField.getValue());
            });
            answersLayout.add(textField, submit);
        }
    }

    private void renderQuestionLayout(QuizQuestionModel questionModel, int roundNumber) {
        questionLayout.removeAll();
        questionLayout.setSpacing(false);
        questionLayout.setPadding(false);
        questionLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        Paragraph textParagraph = new Paragraph();
        textParagraph.setText(questionModel.getText());
        textParagraph.setWidthFull();
        textParagraph.getStyle().set("white-space", "pre-line");

        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getPhotoFilename()));
            image.setMaxHeight("25em");
            questionLayout.add(image);
            textParagraph.addClassNames(LumoUtility.FontSize.LARGE);
        } else {
            textParagraph.addClassNames(LumoUtility.FontSize.XLARGE);
        }

        Span categorySpan = new Span(questionModel.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.SMALL);
        questionLayout.add(categorySpan);

        questionLayout.add(textParagraph);

        if (StringUtils.isNotEmpty(questionModel.getAudioFilename())) {
            Button playAudioButton = new Button("Слушать");
            playAudioButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);
            playAudioButton.setIcon(VaadinIcon.PLAY_CIRCLE.create());
            playAudioButton.addClickListener(event -> {
                AudioUtils.playSoundAsync(questionModel.getAudioFilename());
            });
            questionLayout.add(playAudioButton);
        }
    }

    private void renderCategoriesTable(Map<String, List<QuizQuestionModel>> data) {
        categoriesLayout.removeAll();
        categoriesLayout.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.LIGHT);
        categoriesLayout.setAlignItems(Alignment.CENTER);
        categoriesLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        categoriesLayout.setSpacing(true);

        data.forEach((category, questions) -> {
            Button btn = new Button(category + " (" + questions.size() + ")");
            btn.setWidthFull();
            btn.addClickListener(event -> {
                QuizQuestionModel question = questions.stream().findFirst().orElseThrow();
                Dialog dialog = CleverestComponents.defaultDialog(new Span(question.getText()), "Вопрос", () -> {
                    showCorrectAnswer(question,
                            broadcastService.getState(gameId).getSortedByScoreUsers(),
                            false,
                            true,
                            uName -> {
                                broadcastService.getState(gameId).getUsers().get(uName).increaseScore();
                                broadcastService.updateHistory(gameId, question, uName, true);
                            },
                            () -> broadcastService.sendRenderCategoriesEvent(gameId, category, question));
                });
                dialog.open();
            });
            categoriesLayout.add(btn);
        });
    }

    private void updateUserPersonalScore() {
        personalScoreContainer.removeAll();
        personalScoreContainer.add(CleverestComponents.userScore(
                QuizUtils.getLoggedUser(),
                broadcastService.getState(gameId).getUsers().get(QuizUtils.getLoggedUser()).getScore(),
                LumoUtility.FontSize.LARGE
        ));
    }

    private void showUsersScore(QuizQuestionModel question, boolean roundOver, Runnable onCloseAction) {
        usersScoreLayout.removeAll();
        broadcastService.getState(gameId).getSortedByScoreUsers().forEach((username, userGameState) -> {
            usersScoreLayout.add(CleverestComponents.userScore(username, userGameState.getScore(), LumoUtility.FontSize.XXXLARGE));
        });
        Dialog usersScoreDialog = CleverestComponents.defaultDialog(
                usersScoreLayout,
                "",
                () -> {
                    broadcastService.updateHistoryBatchAndSendEvent(gameId, question);
                    if (roundOver) {
                        broadcastService.sendNewRoundEvent(gameId);
                        return;
                    }
                    onCloseAction.run();
                });
        usersScoreDialog.open();
    }

    private void showCorrectAnswer(QuizQuestionModel question,
                                   Map<String, CleverestGameState.UserGameState> users,
                                   boolean roundOver,
                                   boolean appendApproveButton,
                                   Consumer<String> onApproveAction,
                                   Runnable onCloseAction) {
        AudioUtils.playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
        VerticalLayout answers = new VerticalLayout();
        answers.setSpacing(true);
        answers.setDefaultHorizontalComponentAlignment(Alignment.START);
        answers.setAlignItems(Alignment.CENTER);

        answers.add(CleverestComponents.correctAnswerSpan(question, LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD));
        users.forEach((username, userGameState) -> {
            Span userAnswer = CleverestComponents
                    .userAnswer(username + "(" + userGameState.getLastResponseTime() + ")",
                            userGameState.getLastAnswerText(), LumoUtility.FontSize.XXXLARGE);
            if (!appendApproveButton) {
                answers.add(userAnswer);
            } else {
                Button approveButton = new Button();
                approveButton.addThemeVariants(
                        ButtonVariant.LUMO_SUCCESS,
                        ButtonVariant.LUMO_PRIMARY,
                        ButtonVariant.LUMO_SMALL);
                approveButton.setIcon(VaadinIcon.CHECK.create());
                approveButton.addClickListener(event -> {
                    event.getSource().setEnabled(false);
                    onApproveAction.accept(username);
                });
                answers.add(new Span(userAnswer, approveButton));
            }
            answers.add(new Hr());
        });

        Dialog revealAnswerDialog = CleverestComponents.defaultDialog(
                answers,
                "Ответы",
                () -> {
                    showUsersScore(question, roundOver, onCloseAction);
                    broadcastService.sendUpdatePersonalScoreEvent();
                });
        revealAnswerDialog.open();
    }

    private void renderResults() {
        removeAll();
        add(new Span("finish..."));
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
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.SUBMIT_ANSWER_AUDIOS.next())
                    .thenRun(() -> {
                        QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {

                            boolean approveManually = event.getCurrentRoundNumber() == 2
                                    || event.getQuestion().isSpecial();
                            showCorrectAnswer(
                                    event.getQuestion(),
                                    broadcastService.getState(gameId).getSortedByResponseTimeUsers(),
                                    event.isRoundOver(),
                                    approveManually,
                                    uName -> broadcastService.getState(gameId).getUsers().get(uName).increaseScore(),
                                    () -> broadcastService.sendNextQuestionEvent(gameId));
                        });
                    });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.NextQuestionEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                renderQuestion(event.getQuestion(), event.getRoundNumber());
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.RenderCategoriesEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                questionLayout.removeAll();
                answersLayout.removeAll();
                if (isAdmin) {
                    System.out.println(event.getClass().getName() + " - " + QuizUtils.getLoggedUser());
                    renderCategoriesTable(event.getData());
                } else {
                    questionLayout.add(new Span("Внимание на экран..."));
                }
            });
        }));


        subs.add(broadcastService.subscribe(CleverestBroadcastService.NextRoundEvent.class, event -> {
            if (!isAdmin) {
                return;
            }
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                renderRoundRules(event.getRoundNumber());
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