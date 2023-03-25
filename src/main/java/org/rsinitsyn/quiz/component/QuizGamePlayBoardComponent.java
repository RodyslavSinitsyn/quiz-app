package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Iterator;
import java.util.Set;
import lombok.Getter;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.AudioService;

public class QuizGamePlayBoardComponent extends VerticalLayout implements BeforeLeaveObserver {

    private Paragraph questionTextParagraph;
    private QuizGameAnswersComponent answersComponent;
    private ProgressBar progressBar;

    private QuizGameStateModel quizGameStateModel;

    private Iterator<QuizQuestionModel> questionIterator;
    private QuizQuestionModel currQuestion;
    private int questionNumber = 0;

    public QuizGamePlayBoardComponent(QuizGameStateModel quizGameStateModel) {
        this.quizGameStateModel = quizGameStateModel;
        this.questionIterator = quizGameStateModel.getQuestions().iterator();
        renderQuestion();
    }

    private void renderQuestion() {
        if (!questionIterator.hasNext()) {
            finishGame();
            return;
        }
        currQuestion = questionIterator.next();

        removeAll();
        questionTextParagraph = createQuestionParagraph(currQuestion);
        answersComponent = createAnswersComponent(currQuestion.getAnswers());
        progressBar = createProgressBar(questionNumber++);

        add(questionTextParagraph, answersComponent, progressBar);
    }

    private void finishGame() {
        removeAll();
        closeOpenResources();
        quizGameStateModel.setFinished(true);
        add(new Span("Game over!"));
        fireEvent(new FinishGameEvent(this, quizGameStateModel));
    }


    private QuizGameAnswersComponent createAnswersComponent(Set<QuizQuestionModel.QuizAnswerModel> answers) {
        var answersComponent = new QuizGameAnswersComponent(answers);
        answersComponent.addListener(QuizGameAnswersComponent.AnswerChoosenEvent.class, event -> {
            validateAnswer(event.getAnswer());
            renderQuestion();
        });
        return answersComponent;
    }


    @SneakyThrows
    private Paragraph createQuestionParagraph(QuizQuestionModel questionModel) {
        Paragraph paragraph = new Paragraph();
        paragraph.addClassNames(LumoUtility.AlignSelf.CENTER);
        paragraph.setText(questionModel.getText());

        // TODO Refactor
        if (questionModel.getType().equals(QuestionType.PHOTO)) {
            Image image = new Image();
            image.addClassNames(LumoUtility.AlignSelf.CENTER);
            image.setSrc(new StreamResource(
                    questionModel.getPhotoFilename(),
                    () -> questionModel.openStream()));
            image.setWidth("20em");
            add(image);

            paragraph.addClassNames(LumoUtility.FontSize.XLARGE);
        } else {
            paragraph.addClassNames(LumoUtility.FontSize.XXLARGE);
        }

        return paragraph;
    }

    private ProgressBar createProgressBar(int value) {
        var progressBar = new ProgressBar();
        progressBar.setMax(quizGameStateModel.getQuestions().size());
        progressBar.setMin(0);
        progressBar.setValue(value);
        return progressBar;
    }

    private void validateAnswer(QuizQuestionModel.QuizAnswerModel answerModel) {
        boolean correct = answerModel.isCorrect();
        NotificationVariant variant;
        if (correct) {
            quizGameStateModel.getCorrect().add(currQuestion);
            new AudioService().playSound("correct-answer-1.mp3");
            variant = NotificationVariant.LUMO_SUCCESS;
        } else {
            new AudioService().playSound("wrong-answer-1.mp3");
            variant = NotificationVariant.LUMO_ERROR;
        }
        String notifyText = correct ? "Правильный ответ!" : "Неверно...";
        Notification notification = Notification.show(notifyText, 3_000, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(variant);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (!quizGameStateModel.isFinished()) {
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
        quizGameStateModel.getQuestions().forEach(QuizQuestionModel::closePhotoStream);
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

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
