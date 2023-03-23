package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.rsinitsyn.quiz.model.GameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.AudioService;

public class PlayGameComponent extends VerticalLayout {

    private Paragraph questionTextParagraph;
    private VerticalLayout answersLayout;

    private GameStateModel gameStateModel;

    private Iterator<QuizQuestionModel> questionIterator;
    private QuizQuestionModel currQuestion;

    public PlayGameComponent(GameStateModel gameStateModel) {
        this.gameStateModel = gameStateModel;
        this.questionIterator = gameStateModel.getQuestions().iterator();
        updateQuestions();
    }

    private void updateQuestions() {
        if (!questionIterator.hasNext()) {
            removeAll();
            add(new Span("Game over!"));
            fireEvent(new FinishGameEvent(this, gameStateModel));
            return;
        }
        currQuestion = questionIterator.next();

        removeAll();
        questionTextParagraph = createQuestionParagraph(currQuestion.getText());
        answersLayout = createAnswersLayout(currQuestion.getAnswers());
        add(questionTextParagraph, answersLayout);
    }


    private Paragraph createQuestionParagraph(String questionText) {
        Paragraph paragraph = new Paragraph();
        paragraph.setText(questionText);
        return paragraph;
    }

    private VerticalLayout createAnswersLayout(Set<QuizQuestionModel.QuizAnswerModel> answerSet) {
        VerticalLayout layout = new VerticalLayout();
        List<QuizQuestionModel.QuizAnswerModel> answerList = new ArrayList<>(answerSet);
        Collections.shuffle(answerList);
        answerList.forEach(quizAnswerModel -> layout.add(createAnswerButton(quizAnswerModel)));
        layout.setAlignItems(Alignment.STRETCH);
        return layout;
    }

    private Button createAnswerButton(QuizQuestionModel.QuizAnswerModel answer) {
        var button = new Button(answer.getText());
        button.getElement().setProperty("correct", answer.isCorrect());
        button.addClickListener(event -> {
            validateAnswer(event.getSource());
        });
        return button;
    }

    private void validateAnswer(Button answerSource) {
        boolean correct = Boolean.parseBoolean(answerSource.getElement().getProperty("correct"));
        if (correct) {
            gameStateModel.getCorrect().add(currQuestion);
            new AudioService().playSound("correct-answer-1.mp3");
        } else {
            new AudioService().playSound("wrong-answer-1.mp3");
        }
        String notifyText = correct ? "Good!" : "Wrong...";
        Notification.show(notifyText, 2_000, Notification.Position.MIDDLE);
        updateQuestions();
    }

    @Getter
    public static class FinishGameEvent extends ComponentEvent<PlayGameComponent> {
        private GameStateModel model;

        public FinishGameEvent(PlayGameComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public FinishGameEvent(PlayGameComponent source, GameStateModel model) {
            this(source, false);
            this.model = model;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
