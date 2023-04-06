package org.rsinitsyn.quiz.component.quiz;

import com.google.common.collect.Iterables;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.utils.AudioUtils;

public class QuizGamePlayBoardComponent extends VerticalLayout implements BeforeLeaveObserver {

    private static final Iterator<String> REVEAL_ANSWER_AUDIOS = Iterables.cycle(
            "reveal-answer-1.mp3", "reveal-answer-2.mp3").iterator();

    private static final Iterator<String> SUBMIT_ANSWER_AUDIOS = Iterables.cycle(
                    "submit-answer-1.mp3",
                    "submit-answer-2.mp3",
                    "submit-answer-3.mp3")
            .iterator();

    private static final Iterator<String> CORRECT_ANSWER_AUDIOS = Iterables.cycle(
                    "correct-answer-1.mp3",
                    "correct-answer-2.mp3",
                    "correct-answer-3.mp3",
                    "correct-answer-4.mp3",
                    "correct-answer-5.mp3")
            .iterator();

    private static final Iterator<String> WRONG_ANSWER_AUDIOS = Iterables.cycle(
                    "wrong-answer-1.mp3",
                    "wrong-answer-2.mp3")
            .iterator();

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
        setEnabled(true);

        currQuestion = gameState.getNextQuestion();
        if (currQuestion == null) {
            finishGame();
            return;
        }
        removeAll();

        if (currQuestion.isOptionsOnly() || gameState.isAnswerOptionsEnabled()) {
            questionLayout = createQuestionLayout();
            hintsLayout = createHintsLayout();
            answersComponent = createAnswersComponent();
            add(questionLayout, hintsLayout, answersComponent);
        } else {
            questionLayout = createQuestionLayout();
            add(questionLayout, createRevealButton());
        }

        progressBarLabel = createProgressBarLabel();
        progressBar = createProgressBar();

        add(progressBarLabel, progressBar);

        setPadding(false);
    }

    private Button createRevealButton() {
        Button button = new Button("Узнать ответ");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClassNames(LumoUtility.AlignSelf.CENTER);

        Paragraph correctAnswersSpan = new Paragraph(currQuestion.getAnswers()
                .stream()
                .filter(QuizQuestionModel.QuizAnswerModel::isCorrect)
                .map(QuizQuestionModel.QuizAnswerModel::getText)
                .collect(Collectors.joining(System.lineSeparator())));
        correctAnswersSpan.setId("bla-bla");
        correctAnswersSpan.getStyle().set("white-space", "pre-line");
        correctAnswersSpan.addClassNames(LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.TextAlignment.CENTER);
        correctAnswersSpan.setWidthFull();

        Runnable revealAction = () -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setCancelable(false);
            dialog.setCloseOnEsc(false);

            dialog.setConfirmText("Верный ответ!");
            dialog.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName() + " " + ButtonVariant.LUMO_PRIMARY.getVariantName());
            dialog.setText(correctAnswersSpan);
            dialog.addConfirmListener(event -> {
                fireEvent(
                        new QuizGamePlayBoardComponent.SubmitAnswerEvent(
                                this,
                                currQuestion,
                                currQuestion.getAnswers().stream().filter(QuizQuestionModel.QuizAnswerModel::isCorrect).collect(Collectors.toSet())));
                dialog.close();
                gameState.incrementCorrectAnswersCounter();
                renderQuestion();
            });

            dialog.setRejectText("Неверно...");
            dialog.setRejectable(true);
            dialog.setRejectButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName() + " " + ButtonVariant.LUMO_PRIMARY.getVariantName());
            dialog.addRejectListener(event -> {
                fireEvent(
                        new QuizGamePlayBoardComponent.SubmitAnswerEvent(
                                this,
                                currQuestion,
                                Collections.singleton(currQuestion.getAnswers().stream().filter(m -> !m.isCorrect()).findFirst().orElseThrow())));
                dialog.close();
                renderQuestion();
            });
            dialog.open();
            AudioUtils.playStaticSoundAsync(REVEAL_ANSWER_AUDIOS.next());
        };
        button.addClickListener(event -> {
            if (!gameState.isIntrigueEnabled()) {
                revealAction.run();
                return;
            }
            showIntrigueAndRunAction(revealAction);
        });
        return button;
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
                gameState.setRevelCountHintUsed(true);
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

    private QuizGameAnswersComponent createAnswersComponent() {
        var answersComponent = new QuizGameAnswersComponent(currQuestion.getShuffledAnswers(), currQuestion.getType());
        answersComponent.addListener(QuizGameAnswersComponent.AnswerChoosenEvent.class, event -> {
            if (!gameState.isIntrigueEnabled()) {
                submitOptionableAnswer(event.getAnswers());
                return;
            }
            showIntrigueAndRunAction(() -> submitOptionableAnswer(event.getAnswers()));
        });
        return answersComponent;
    }

    @SneakyThrows
    private VerticalLayout createQuestionLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        Paragraph textParagraph = new Paragraph();
        textParagraph.setText(currQuestion.getText());
        textParagraph.getStyle().set("white-space", "pre-line");

        if (StringUtils.isNotEmpty(currQuestion.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(new StreamResource(
                    currQuestion.getPhotoFilename(),
                    () -> currQuestion.openStream()));
            image.setMaxHeight("25em");
            layout.add(image);
            textParagraph.addClassNames(LumoUtility.FontSize.XXLARGE);
        } else {
            textParagraph.addClassNames(LumoUtility.FontSize.XXXLARGE);
        }

        Span categorySpan = new Span(currQuestion.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.MEDIUM);
        layout.add(categorySpan);

        layout.add(textParagraph);

        if (StringUtils.isNotEmpty(currQuestion.getAudioFilename())) {
            Button playAudioButton = new Button("Слушать");
            playAudioButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);
            playAudioButton.setIcon(VaadinIcon.PLAY_CIRCLE.create());
            playAudioButton.addClickListener(event -> {
                AudioUtils.playSoundAsync(currQuestion.getAudioFilename());
            });
            layout.add(playAudioButton);
        }

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

    private void submitOptionableAnswer(Set<QuizQuestionModel.QuizAnswerModel> answers) {
        verifyAnswerAndNotify(answers);
        fireEvent(new SubmitAnswerEvent(this, currQuestion, answers));
        renderQuestion();
    }

    private void verifyAnswerAndNotify(Set<QuizQuestionModel.QuizAnswerModel> answerModels) {
        boolean correct = currQuestion.areAnswersCorrect(answerModels);
        NotificationVariant variant;
        if (correct) {
            gameState.incrementCorrectAnswersCounter();
            AudioUtils.playStaticSoundAsync(CORRECT_ANSWER_AUDIOS.next());
            variant = NotificationVariant.LUMO_SUCCESS;
        } else {
            AudioUtils.playStaticSoundAsync(WRONG_ANSWER_AUDIOS.next());
            variant = NotificationVariant.LUMO_ERROR;
        }
        String notifyText = correct ? "Правильный ответ!" : "Неверно...";
        Notification notification = Notification.show(notifyText, 3_000, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(variant);
    }

    private void showIntrigueAndRunAction(Runnable action) {
        setEnabled(false);
        Notification notification = Notification.show("Ответ принят...", 3_000, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        AudioUtils.playStaticSoundAsync(SUBMIT_ANSWER_AUDIOS.next())
                .thenRun(() -> {
                    getUI().ifPresent(ui -> ui.access(action::run));
                });
    }

    private void finishGame() {
        removeAll();
        closeOpenResources();
        gameState.setStatus(GameStatus.FINISHED);
        fireEvent(new FinishGameEvent(this, gameState));
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
