package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractAnswersLayout extends VerticalLayout {

    protected final QuestionModel question;
    protected final List<QuestionModel.AnswerModel> copiedAnswerList;

    // Components
    protected final Button submitButton = CleverestComponents.submitButton(e -> {
    });

    public AbstractAnswersLayout(QuestionModel question) {
        this.question = question;
        this.copiedAnswerList = new ArrayList<>(question.getShuffledAnswers());
        setAlignItems(Alignment.STRETCH);
        renderComponents();
    }

    private void renderComponents() {
        renderAnswers();
        renderSubmitButton();
    }

    protected void renderSubmitButton() {
        submitButton.addClickListener(this::submitHandler);
        submitButton.setEnabled(isSubmitButtonEnabled());
    }

    protected abstract void renderAnswers();

    protected abstract void submitHandler(ClickEvent<Button> event);

    protected abstract boolean isSubmitButtonEnabled();

    public void removeWrongAnswersAndRerender(int answersToRemove) {
        copiedAnswerList.removeAll(copiedAnswerList.stream()
                .filter(answerModel -> !answerModel.isCorrect())
                .limit(answersToRemove)
                .toList());
        renderComponents();
    }

    @Getter
    public static class AnswerChosenEvent<T extends AbstractAnswersLayout> extends ComponentEvent<T> {
        private Set<QuestionModel.AnswerModel> answers;
        private boolean isCorrect;
        private boolean manuallyApprove = false;

        public AnswerChosenEvent(T source, boolean fromClient) {
            super(source, fromClient);
        }

        public AnswerChosenEvent(T source,
                                 Set<QuestionModel.AnswerModel> answers,
                                 boolean isCorrect) {
            this(source, true);
            this.answers = answers;
            this.isCorrect = isCorrect;
        }

        public AnswerChosenEvent(T source,
                                 Set<QuestionModel.AnswerModel> answers,
                                 boolean isCorrect,
                                 boolean manuallyApprove) {
            this(source, true);
            this.answers = answers;
            this.isCorrect = isCorrect;
            this.manuallyApprove = manuallyApprove;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
