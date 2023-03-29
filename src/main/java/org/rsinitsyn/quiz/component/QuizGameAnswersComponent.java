package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import lombok.Getter;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

public class QuizGameAnswersComponent extends VerticalLayout {

    private List<QuizQuestionModel.QuizAnswerModel> answerList;

    public QuizGameAnswersComponent(List<QuizQuestionModel.QuizAnswerModel> answerList) {
        this.answerList = answerList;
        renderAnswers();
        setAlignItems(Alignment.STRETCH);
    }

    private void renderAnswers() {
        answerList.forEach(quizAnswerModel -> add(createAnswerButton(quizAnswerModel)));
    }

    private Button createAnswerButton(QuizQuestionModel.QuizAnswerModel answer) {
        var button = new Button(answer.getText());
        button.addClassNames(LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.BOLD);
        button.addClickListener(event -> {
            fireEvent(new AnswerChoosenEvent(this, answer));
        });
        return button;
    }

    public void removeWrongAnswersAndRerender(int answersToRemove) {
        removeAll();
        answerList.removeAll(answerList.stream()
                .filter(answerModel -> !answerModel.isCorrect())
                .limit(answersToRemove)
                .toList());
        renderAnswers();
    }

    @Getter
    public static class AnswerChoosenEvent extends ComponentEvent<QuizGameAnswersComponent> {
        private QuizQuestionModel.QuizAnswerModel answer;

        public AnswerChoosenEvent(QuizGameAnswersComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public AnswerChoosenEvent(QuizGameAnswersComponent source, QuizQuestionModel.QuizAnswerModel answer) {
            this(source, false);
            this.answer = answer;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
