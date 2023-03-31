package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Set;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.utils.AudioUtils;

public class QuizGamePlayBoardComponent extends VerticalLayout implements BeforeLeaveObserver {

    private VerticalLayout questionLayout = new VerticalLayout();
    private QuizGameAnswersComponent answersComponent;
    private HorizontalLayout hintsLayout = new HorizontalLayout();
    private Div progressBarLabel = new Div();
    private ProgressBar progressBar = new ProgressBar();

    private QuizGameStateModel gameState;
    private QuizQuestionModel currQuestion;

    public QuizGamePlayBoardComponent(QuizGameStateModel gameState) {
        this.gameState = gameState;
        renderQuestion();
    }

    private void renderQuestion() {
        currQuestion = gameState.getNextQuestion();
        if (currQuestion == null) {
            finishGame();
            return;
        }
        removeAll();
        questionLayout = createQuestionLayout();
        hintsLayout = createHintsLayout();
        answersComponent = createAnswersComponent();
        progressBarLabel = createProgressBarLabel();
        progressBar = createProgressBar();

        add(questionLayout, hintsLayout, answersComponent, progressBarLabel, progressBar);

        setPadding(false);
    }

    private HorizontalLayout createHintsLayout() {
        var layout = new HorizontalLayout();

        if (!gameState.isHintsEnabled()) {
            layout.setVisible(false);
            return layout;
        }

        if (currQuestion.getType().equals(QuestionType.MULTI)) {
            Button revealCountHint = new Button("Количество ответов");
            revealCountHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            revealCountHint.setEnabled(!gameState.isRevelCountHintUsed());
            revealCountHint.addClickListener(event -> {
                revealCountHint.setText("Верных ответов: "
                        + currQuestion.getAnswers().stream().filter(QuizQuestionModel.QuizAnswerModel::isCorrect).count());
                revealCountHint.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            });
            layout.add(revealCountHint);
            return layout;
        }

        // For QuestionType.TEXT
        Button halfHint = new Button("50 на 50");
        halfHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        halfHint.addClickListener(event -> {
            gameState.setHalfHintUsed(true);
            answersComponent.removeWrongAnswersAndRerender(2);
            layout.getChildren().forEach(component -> ((Button) component).setEnabled(false));
        });
        halfHint.setEnabled(!gameState.isHalfHintUsed());

        Button threeLeftHint = new Button("3/4");
        threeLeftHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        threeLeftHint.addClickListener(event -> {
            gameState.setThreeLeftHintUsed(true);
            answersComponent.removeWrongAnswersAndRerender(1);
            layout.getChildren().forEach(component -> ((Button) component).setEnabled(false));
        });
        threeLeftHint.setEnabled(!gameState.isThreeLeftHintUsed());

        layout.add(halfHint, threeLeftHint);
        layout.setAlignItems(Alignment.CENTER);
        return layout;
    }

    private void finishGame() {
        removeAll();
        closeOpenResources();
        gameState.setStatus(GameStatus.FINISHED);
        fireEvent(new FinishGameEvent(this, gameState));
    }

    private QuizGameAnswersComponent createAnswersComponent() {
        var answersComponent = new QuizGameAnswersComponent(currQuestion.getShuffledAnswers(), currQuestion.getType());
        answersComponent.addListener(QuizGameAnswersComponent.AnswerChoosenEvent.class, event -> {
            if (!gameState.isIntrigueEnabled()) {
                submitAnswer(event.getAnswers());
                return;
            }
            setEnabled(false);
            Notification notification = Notification.show("Ответ принят...", 3_000, Notification.Position.TOP_STRETCH);
            notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            AudioUtils.playSoundAsync("submit-answer-1.mp3")
                    .thenRun(() -> {
                        getUI().ifPresent(ui -> {
                            ui.access(() -> {
                                submitAnswer(event.getAnswers());
                            });
                        });
                    });
        });
        return answersComponent;
    }

    private void submitAnswer(Set<QuizQuestionModel.QuizAnswerModel> answers) {
        setEnabled(true);
        validateAnswer(answers);
        fireEvent(new SubmitAnswerEvent(this, currQuestion, answers));
        renderQuestion();
    }

    @SneakyThrows
    private VerticalLayout createQuestionLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);

        Paragraph paragraph = new Paragraph();
        paragraph.addClassNames(LumoUtility.AlignSelf.CENTER);
        paragraph.setText(currQuestion.getText());
        paragraph.getStyle().set("white-space", "pre-line");

        if (StringUtils.isNotEmpty(currQuestion.getPhotoFilename())) {
            Image image = new Image();
            image.addClassNames(LumoUtility.AlignSelf.CENTER);
            image.setSrc(new StreamResource(
                    currQuestion.getPhotoFilename(),
                    () -> currQuestion.openStream()));
            image.setMaxHeight("25em");
//            image.addClickListener(event -> {
//                ConfirmDialog dialog = new ConfirmDialog();
//                dialog.setSizeFull();
//                dialog.setCancelable(false);
//                dialog.setRejectable(false);
//                dialog.setCloseOnEsc(true);
//                dialog.setConfirmText("...");
//                dialog.addCancelListener(e -> {
//                    e.getSource().close();
//                    e.getSource().remove(image);
//                    image.setWidth("25em");
//                    layout.addComponentAsFirst(image);
//                });
//                image.setHeightFull();
//                image.setWidth("auto");
//                image.setMaxHeight("100%");
//                image.setMaxWidth("100%");
//                dialog.add(image);
//                dialog.setSizeUndefined();
//                dialog.open();
//            });
            layout.add(image);
            paragraph.addClassNames(LumoUtility.FontSize.XLARGE);
        } else {
            paragraph.addClassNames(LumoUtility.FontSize.XXXLARGE);
        }
        layout.add(paragraph);

        return layout;
    }

    private Div createProgressBarLabel() {
        var label = new Div();
        label.setText(String.format("Ответов (%d/%d)",
                gameState.getCurrentQuestionNumber() - 1,
                gameState.getQuestionsCount()));
        return label;
    }

    private ProgressBar createProgressBar() {
        var progressBar = new ProgressBar();
        progressBar.setMax(gameState.getQuestionsCount());
        progressBar.setMin(0);
        progressBar.setValue(gameState.getCurrentQuestionNumber() - 1);
        return progressBar;
    }

    private void validateAnswer(Set<QuizQuestionModel.QuizAnswerModel> answerModels) {
        boolean correct = currQuestion.areAnswersCorrect(answerModels);
        NotificationVariant variant;
        if (correct) {
            gameState.getCorrect().add(currQuestion);
            AudioUtils.playSoundAsync("correct-answer-1.mp3");
            variant = NotificationVariant.LUMO_SUCCESS;
        } else {
            AudioUtils.playSoundAsync("wrong-answer-1.mp3");
            variant = NotificationVariant.LUMO_ERROR;
        }
        String notifyText = correct ? "Правильный ответ!" : "Неверно...";
        Notification notification = Notification.show(notifyText, 3_000, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(variant);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (!GameStatus.FINISHED.equals(gameState.getStatus())) {
            BeforeLeaveEvent.ContinueNavigationAction leaveAction =
                    event.postpone();
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setText("Покинув страницу придется начать с начала!");
            confirmDialog.setCancelable(true);
            confirmDialog.addConfirmListener(e -> leaveAction.proceed());
            confirmDialog.open();
        }
        closeOpenResources();
    }

    private void closeOpenResources() {
        gameState.getQuestions().forEach(QuizQuestionModel::closePhotoStream);
    }


    @Getter
    public static class FinishGameEvent extends ComponentEvent<QuizGamePlayBoardComponent> {
        private QuizGameStateModel model;

        public FinishGameEvent(QuizGamePlayBoardComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public FinishGameEvent(QuizGamePlayBoardComponent source, QuizGameStateModel model) {
            this(source, false);
            this.model = model;
        }
    }

    @Getter
    public static class SubmitAnswerEvent extends ComponentEvent<QuizGamePlayBoardComponent> {
        private QuizQuestionModel question;
        private Set<QuizQuestionModel.QuizAnswerModel> answer;

        public SubmitAnswerEvent(QuizGamePlayBoardComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public SubmitAnswerEvent(QuizGamePlayBoardComponent source, QuizQuestionModel question, Set<QuizQuestionModel.QuizAnswerModel> answer) {
            this(source, false);
            this.question = question;
            this.answer = answer;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
