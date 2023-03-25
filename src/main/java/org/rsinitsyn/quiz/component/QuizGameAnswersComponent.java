package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

public class QuizGameAnswersComponent extends VerticalLayout {

    private Set<QuizQuestionModel.QuizAnswerModel> answerSet;

    public QuizGameAnswersComponent(Set<QuizQuestionModel.QuizAnswerModel> answerSet) {
        this.answerSet = answerSet;
        List<QuizQuestionModel.QuizAnswerModel> answerList = new ArrayList<>(answerSet);
        Collections.shuffle(answerList);
        answerList.forEach(quizAnswerModel -> add(createAnswerButton(quizAnswerModel)));
        setAlignItems(Alignment.STRETCH);
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
