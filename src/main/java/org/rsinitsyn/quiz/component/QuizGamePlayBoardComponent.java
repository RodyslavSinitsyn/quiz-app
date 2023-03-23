package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.shared.Registration;
import java.util.Iterator;
import java.util.Set;
import lombok.Getter;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.AudioService;

public class QuizGamePlayBoardComponent extends VerticalLayout {

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
            removeAll();
            add(new Span("Game over!"));
            fireEvent(new FinishGameEvent(this, quizGameStateModel));
            return;
        }
        currQuestion = questionIterator.next();

        removeAll();
        questionTextParagraph = createQuestionParagraph(currQuestion.getText());
        answersComponent = createAnswersComponent(currQuestion.getAnswers());
        progressBar = createProgressBar(questionNumber++);

        add(questionTextParagraph, answersComponent, progressBar);
    }

    private QuizGameAnswersComponent createAnswersComponent(Set<QuizQuestionModel.QuizAnswerModel> answers) {
        var answersComponent = new QuizGameAnswersComponent(answers);
        answersComponent.addListener(QuizGameAnswersComponent.AnswerChoosenEvent.class, event -> {
            validateAnswer(event.getAnswer());
            renderQuestion();
        });
        return answersComponent;
    }


    private Paragraph createQuestionParagraph(String questionText) {
        Paragraph paragraph = new Paragraph();
        paragraph.setText(questionText);
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
        if (correct) {
            quizGameStateModel.getCorrect().add(currQuestion);
            new AudioService().playSound("correct-answer-1.mp3");
        } else {
            new AudioService().playSound("wrong-answer-1.mp3");
        }
        String notifyText = correct ? "Good!" : "Wrong...";
        Notification.show(notifyText, 2_000, Notification.Position.MIDDLE);
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
