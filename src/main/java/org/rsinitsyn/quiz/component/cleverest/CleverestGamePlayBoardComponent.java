package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.CleverestBroadcastService;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private final String gameId;
    private final CleverestBroadcastService broadcastService;
    private final boolean isAdmin;

    private VerticalLayout usersScoreLayout = new VerticalLayout();
    private VerticalLayout questionLayout = new VerticalLayout();
    private VerticalLayout answersLayout = new VerticalLayout();

    private List<Registration> subs = new ArrayList<>();

    public CleverestGamePlayBoardComponent(String gameId, CleverestBroadcastService broadcastService, boolean isAdmin) {
        this.gameId = gameId;
        this.broadcastService = broadcastService;
        this.isAdmin = isAdmin;

        renderRoundRules(broadcastService.getState(gameId).getRoundNumber());
        renderQuestion(broadcastService.getState(gameId).getRoundNumber());

        add(usersScoreLayout, questionLayout, answersLayout);
    }

    private void renderRoundRules(int roundNumber) {
        if (isAdmin) {
            String roundNumberText = "Раунд " + roundNumber;
            Notification notification = Notification.show(roundNumberText, 3_000, Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        }
    }

    private void renderQuestion(int roundNumber) {
        QuizQuestionModel questionModel = broadcastService.getState(gameId).getCurrent();
        if (questionModel == null) {
            new Span("Finish...");
            return;
        }
        setEnabled(true);
        renderQuestionLayout(questionModel, roundNumber);
        renderOptions(questionModel, roundNumber);
        updateUsersScore();
    }

    private void renderOptions(QuizQuestionModel questionModel, int roundNumber) {
        answersLayout.removeAll();
        if (roundNumber == 1) {
            questionModel.getAnswers().forEach(answerModel -> {
                Button button = new Button();
                button.setText(answerModel.getText());
                button.addClickListener(event -> {
                    broadcastService.submitAnswer(gameId, QuizUtils.getLoggedUser(), answerModel);
                });
                answersLayout.add(button);
            });

        } else if (roundNumber == 2) {

            if (!isAdmin) {
                TextField textField = new TextField("Введите ответ");
                Button submit = new Button("Ответить");

                textField.setValueChangeMode(ValueChangeMode.EAGER);
                textField.addValueChangeListener(event -> submit.setEnabled(!event.getValue().isBlank()));

                submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                submit.setEnabled(false);
                submit.addClickListener(event -> {
                    broadcastService.submitAnswer(gameId, QuizUtils.getLoggedUser(), textField.getValue());
                });
                answersLayout.add(textField, submit);
            }
        }
    }

    private void renderQuestionLayout(QuizQuestionModel questionModel, int roundNumber) {
        questionLayout.removeAll();
        questionLayout.setSpacing(false);
        questionLayout.setPadding(false);
        questionLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        Paragraph textParagraph = new Paragraph();
        textParagraph.setText(questionModel.getText());
        textParagraph.getStyle().set("white-space", "pre-line");

        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getPhotoFilename()));
            image.setMaxHeight("25em");
            questionLayout.add(image);
            textParagraph.addClassNames(LumoUtility.FontSize.XXLARGE);
        } else {
            textParagraph.addClassNames(LumoUtility.FontSize.XXXLARGE);
        }

        Span categorySpan = new Span(questionModel.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.MEDIUM);
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

    private void updateUsersScore() {
        usersScoreLayout.removeAll();
        broadcastService.getState(gameId).getUsers().forEach((name, score) -> {
            usersScoreLayout.add(new Span(name + " : " + score.getScore()));
        });
    }


    private void showUserAnswers(int roundNumber, boolean roundOver, boolean gameFinished) {
        Dialog dialog = new Dialog();

        broadcastService.getState(gameId).getUsers().forEach((username, userGameState) -> {
            Span span = new Span();
            span.setText(username + " = " + userGameState.getLatestAnswer());

            Button answerCorrectButton = new Button();
            answerCorrectButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
            answerCorrectButton.setText("+");
            answerCorrectButton.addClickListener(event -> {
                event.getSource().setEnabled(false);
                broadcastService.getState(gameId).getUsers().get(username).increaseScore();
            });
            dialog.setCloseOnOutsideClick(true);
            dialog.addDialogCloseActionListener(event -> {
                updateUsersScore();
                event.getSource().close();
                broadcastService.sendNextQuestionEvent();
                if (gameFinished) {
                    broadcastService.finishGame();
                    return;
                }
                if (roundOver) {
                    renderRoundRules(broadcastService.getState(gameId).getRoundNumber());
                }
            });

            dialog.add(span);
            if (roundNumber == 2) {
                dialog.add(answerCorrectButton);
            }
            dialog.add(new Hr());
        });

        dialog.open();
    }

    private void renderResults() {
        removeAll();
        add(new Span("finish..."));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        subs.add(broadcastService.subscribe(CleverestBroadcastService.UserAnsweredEvent.class, event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                        Notification notification = Notification.show(event.getUsername(), 1_000, Notification.Position.TOP_CENTER);
                        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);

                        if (event.getUsername().equals(QuizUtils.getLoggedUser())) {
                            setEnabled(false);
                        }
                    });
                })
        );

        subs.add(broadcastService.subscribe(CleverestBroadcastService.AllUsersAnsweredEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                if (isAdmin) {
                    showUserAnswers(event.getCurrentRoundNumber(), event.isRoundOver(), event.isGameFinished());
                }
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.NextQuestionEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                renderQuestion(broadcastService.getState(gameId).getRoundNumber());
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.NextRoundEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                if (isAdmin) {
                    renderRoundRules(event.getRoundNumber());
                }
            });
        }));

        subs.add(broadcastService.subscribe(CleverestBroadcastService.GameFinishedEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                renderResults();
            });
        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subs.forEach(Registration::remove);
    }
}