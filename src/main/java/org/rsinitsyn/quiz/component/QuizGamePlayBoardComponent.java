package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
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
import org.rsinitsyn.quiz.service.AudioService;

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
        questionLayout = createQuestionLayour(currQuestion);
        hintsLayout = createHintsLayout();
        answersComponent = createAnswersComponent();
        progressBarLabel = createProgressBarLabel();
        progressBar = createProgressBar();

        add(questionLayout, hintsLayout, answersComponent, progressBarLabel, progressBar);
    }

    private HorizontalLayout createHintsLayout() {
        var layout = new HorizontalLayout();

        if (!gameState.isHintsEnabled() || currQuestion.getType().equals(QuestionType.MULTI)) {
            layout.setVisible(false);
            return layout;
        }

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
        add(new Span("Game over!"));
        fireEvent(new FinishGameEvent(this, gameState));
    }

    private QuizGameAnswersComponent createAnswersComponent() {
        var answersComponent = new QuizGameAnswersComponent(currQuestion.getShuffledAnswers(), currQuestion.getType());
        answersComponent.addListener(QuizGameAnswersComponent.AnswerChoosenEvent.class, event -> {
            validateAnswer(event.getAnswer());
            fireEvent(new SubmitAnswerEvent(this, currQuestion, event.getAnswer()));
            renderQuestion();
        });
        return answersComponent;
    }

    @SneakyThrows
    private VerticalLayout createQuestionLayour(QuizQuestionModel questionModel) {
        VerticalLayout layout = new VerticalLayout();

        Paragraph paragraph = new Paragraph();
        paragraph.addClassNames(LumoUtility.AlignSelf.CENTER);
        paragraph.setText(questionModel.getText());
        paragraph.getStyle().set("white-space", "pre-line");

        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.addClassNames(LumoUtility.AlignSelf.CENTER);
            image.setSrc(new StreamResource(
                    questionModel.getPhotoFilename(),
                    () -> questionModel.openStream()));
            image.setWidth("20em");
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
        boolean correct = answerModels.stream().allMatch(QuizQuestionModel.QuizAnswerModel::isCorrect);
        NotificationVariant variant;
        if (correct) {
            gameState.getCorrect().add(currQuestion);
            new AudioService().playSoundAsync("correct-answer-1.mp3");
            variant = NotificationVariant.LUMO_SUCCESS;
        } else {
            new AudioService().playSoundAsync("wrong-answer-1.mp3");
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
