package org.rsinitsyn.quiz.component.quiz;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.shared.Registration;
import javazoom.jl.player.Player;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.custom.answer.AbstractAnswersLayout;
import org.rsinitsyn.quiz.component.custom.question.BaseQuestionLayout;
import org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.model.AnswerHint;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.rsinitsyn.quiz.utils.StaticValuesHolder.*;

@Slf4j
public class QuizGamePlayBoardComponent extends VerticalLayout implements BeforeLeaveObserver {

    private BaseQuestionLayout questionLayout;
    private Div progressBarLabel = new Div();
    private ProgressBar progressBar = new ProgressBar();

    private QuizGameState gameState;
    private QuestionModel currQuestion;
    private Player lastPlayedAudio = null;

    private List<Registration> subscriptions = new ArrayList<>();

    public QuizGamePlayBoardComponent() {
        setPadding(false);
    }

    public void setState(QuizGameState gameState) {
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

//        lastPlayedAudio = AudioUtils.playStaticAudioAsyncAndGetPlayer(StaticValuesHolder.THINK_AUDIOS.next());

        if (currQuestion.isOptionsOnly() || gameState.isAnswerOptionsEnabled()) {
            questionLayout = createQuestionLayout();
            unsubscribeQuestionLayout();
            subscribeQuestionLayout();
            add(questionLayout);
        } else {
            throw new RuntimeException("WITHOUT OPTIONS NOT WORKING");
//            questionLayout = createQuestionLayout();
//            configureQuestionLayout();
//            add(questionLayout, createRevealButton());
        }

        progressBarLabel = createProgressBarLabel();
        progressBar = createProgressBar();

        add(progressBarLabel, progressBar);
    }

//    private Button createRevealButton() {
//        Button button = new Button("Узнать ответ");
//        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
//        button.addClassNames(LumoUtility.AlignSelf.CENTER);
//
//        Paragraph correctAnswersSpan = new Paragraph(currQuestion.getAnswers()
//                .stream()
//                .filter(QuestionModel.AnswerModel::isCorrect)
//                .map(QuestionModel.AnswerModel::getText)
//                .collect(Collectors.joining(System.lineSeparator())));
//        correctAnswersSpan.setId("bla-bla");
//        correctAnswersSpan.getStyle().set("white-space", "pre-line");
//        correctAnswersSpan.addClassNames(LumoUtility.FontSize.XXXLARGE,
//                LumoUtility.FontWeight.BOLD,
//                LumoUtility.TextAlignment.CENTER);
//        correctAnswersSpan.setWidthFull();
//
//        Runnable revealAction = () -> {
//            ConfirmDialog dialog = new ConfirmDialog();
//            dialog.setCancelable(false);
//            dialog.setCloseOnEsc(false);
//
//            dialog.setConfirmText("Верный ответ!");
//            dialog.setConfirmButtonTheme(ButtonVariant.LUMO_SUCCESS.getVariantName() + " " + ButtonVariant.LUMO_PRIMARY.getVariantName());
//            dialog.setText(correctAnswersSpan);
//            dialog.addConfirmListener(event -> {
//                fireEvent(
//                        new SubmitUserAnswer(
//                                this,
//                                currQuestion,
//                                currQuestion.getAnswers().stream().filter(QuestionModel.AnswerModel::isCorrect).collect(Collectors.toSet())));
//                dialog.close();
//                gameState.incrementCorrectAnswersCounter();
//                renderQuestion();
//            });
//
//            dialog.setRejectText("Неверно...");
//            dialog.setRejectable(true);
//            dialog.setRejectButtonTheme(ButtonVariant.LUMO_ERROR.getVariantName() + " " + ButtonVariant.LUMO_PRIMARY.getVariantName());
//            dialog.addRejectListener(event -> {
//                fireEvent(
//                        new SubmitUserAnswer(
//                                this,
//                                currQuestion,
//                                Collections.singleton(currQuestion.getAnswers().stream()
//                                        .filter(m -> !m.isCorrect())
//                                        .findFirst()
//                                        .orElse(defaultWrong()))));
//                dialog.close();
//                renderQuestion();
//            });
//            dialog.open();
//            AudioUtils.playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
//        };
//        button.addClickListener(event -> {
//            Optional.ofNullable(lastPlayedAudio).ifPresent(Player::close);
//            if (!gameState.isIntrigueEnabled()) {
//                revealAction.run();
//                return;
//            }
//            showIntrigueAndRunAction(revealAction);
//        });
//        return button;
//    }

    private BaseQuestionLayout createQuestionLayout() {
        return QuestionLayoutFactory.get(new QuestionLayoutRequest()
                .question(currQuestion)
                .hintsState(gameState.getHintsState()));
    }

    private void subscribeQuestionLayout() {
        subscriptions.add(questionLayout.addListener(BaseQuestionLayout.QuestionAnsweredEvent.class, event -> {
            Optional.ofNullable(lastPlayedAudio).ifPresent(Player::close);
            if (gameState.isIntrigueEnabled()) {
                showIntrigueAndRunAction(() -> submitAnswer(event));
            } else {
                submitAnswer(event);
            }
        }));
        subscriptions.add(questionLayout.getAnswersLayout().addListener(AbstractAnswersLayout.HintUsedEvent.class, event -> {
            if (event.getHint() == AnswerHint.HALF) {
                gameState.setHalfHintUsed(true);
            } else if (event.getHint() == AnswerHint.THREE) {
                gameState.setThreeLeftHintUsed(true);
            } else if (event.getHint() == AnswerHint.CORRECT_COUNT) {
                gameState.setRevelCountHintUsed(true);
            }
        }));
    }

    private void unsubscribeQuestionLayout() {
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
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

    private void submitAnswer(BaseQuestionLayout.QuestionAnsweredEvent event) {
        calculateScoreAndShowPopup(event.getAnswerChosenEvent().isCorrect());
        fireEvent(new SubmitUserAnswer(this, currQuestion, event.getAnswerChosenEvent().getAnswers(), event.getAnswerChosenEvent().isCorrect()));
        renderQuestion();
    }

    private void calculateScoreAndShowPopup(boolean isCorrect) {
        NotificationVariant popupVariant;
        if (isCorrect) {
            gameState.incrementCorrectAnswersCounter();
            AudioUtils.playStaticSoundAsync(CORRECT_ANSWER_AUDIOS.next());
            popupVariant = NotificationVariant.LUMO_SUCCESS;
        } else {
            AudioUtils.playStaticSoundAsync(WRONG_ANSWER_AUDIOS.next());
            popupVariant = NotificationVariant.LUMO_ERROR;
        }
        String notifyText = isCorrect ? "Правильный ответ!" : "Неверно...";
        Notification notification = Notification.show(notifyText, 3_000, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(popupVariant);
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
        Optional.ofNullable(lastPlayedAudio).ifPresent(Player::close);
        gameState.setStatus(GameStatus.FINISHED);
        fireEvent(new FinishGameEvent(this, gameState));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (!GameStatus.FINISHED.equals(gameState.getStatus())) {
            BeforeLeaveEvent.ContinueNavigationAction leaveAction =
                    event.postpone();
            QuizComponents.openConfirmDialog(
                    new Span("Покинув страницу придется начать с начала!"),
                    "Покинуть игру?",
                    leaveAction::proceed
            );
        }
    }

    @Getter
    public static class FinishGameEvent extends ComponentEvent<QuizGamePlayBoardComponent> {
        private QuizGameState model;

        public FinishGameEvent(QuizGamePlayBoardComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public FinishGameEvent(QuizGamePlayBoardComponent source, QuizGameState model) {
            this(source, false);
            this.model = model;
        }
    }

    @Getter
    public static class SubmitUserAnswer extends ComponentEvent<QuizGamePlayBoardComponent> {
        private final QuestionModel question;
        private final Set<String> answers;
        private final boolean correct;

        public SubmitUserAnswer(QuizGamePlayBoardComponent source,
                                QuestionModel question,
                                Set<String> answers,
                                boolean correct) {
            super(source, false);
            this.question = question;
            this.answers = answers;
            this.correct = correct;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (subscriptions.isEmpty()) {
            subscribeQuestionLayout();
            log.trace("onAttach. subscribe {}", subscriptions.size());
        } else {
            log.trace("onAttach. already subscribed {}", subscriptions.size());
        }
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        log.trace("onDetach. unsubscribe {}", subscriptions.size());
        unsubscribeQuestionLayout();
    }
}
