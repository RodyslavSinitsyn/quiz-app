package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.component.custom.event.StubEvent;
import org.rsinitsyn.quiz.model.AnswerHint;
import org.rsinitsyn.quiz.model.HintsState;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractAnswersLayout extends VerticalLayout {

    protected final QuestionModel question;
    protected final List<QuestionModel.AnswerModel> copiedAnswerList;
    protected HintsState hintsState;

    // Components
    protected final HorizontalLayout hintsLayout = new HorizontalLayout();
    protected final Button submitButton = CleverestComponents.submitButton(e -> {
    });

    public AbstractAnswersLayout(QuestionModel question) {
        this.question = question;
        this.copiedAnswerList = new ArrayList<>(question.getShuffledAnswers());
        this.hintsState = question.getHintsState();
        setAlignItems(Alignment.STRETCH);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderComponents();
    }

    private void renderComponents() {
        renderHintsLayout();
        renderAnswers();
        renderSubmitButton();
    }

    protected void renderSubmitButton() {
        submitButton.addClickListener(this::submitHandler);
        submitButton.setEnabled(false);
        add(submitButton);
    }

    protected abstract void renderAnswers();

    protected abstract void submitHandler(ClickEvent<Button> event);

    protected List<Component> getHintsComponents() {
        return Collections.emptyList();
    }

    protected void renderHintsLayout() {
        hintsLayout.removeAll();
        var hintComponents = getHintsComponents();
        if (!hintsState.isHintsEnabled() || hintComponents.isEmpty()) {
            hintsLayout.setVisible(false);
            return;
        }
        hintsLayout.setAlignItems(Alignment.CENTER);
        hintsLayout.add(getHintsComponents());
        add(hintsLayout);
    }

    protected void removeWrongAnswersAndRerender(int answersToRemove) {
        copiedAnswerList.removeAll(copiedAnswerList.stream()
                .filter(answerModel -> !answerModel.isCorrect())
                .limit(answersToRemove)
                .toList());
        renderAnswers();
    }

    @Getter
    public static class AnswerChosenEvent extends StubEvent {
        private Set<String> answers;
        private boolean isCorrect;
        private boolean manuallyApprove = false;

        public AnswerChosenEvent(Set<String> answers,
                                 boolean isCorrect) {
            this.answers = answers;
            this.isCorrect = isCorrect;
        }

        public AnswerChosenEvent(Set<String> answers,
                                 boolean isCorrect,
                                 boolean manuallyApprove) {
            this.answers = answers;
            this.isCorrect = isCorrect;
            this.manuallyApprove = manuallyApprove;
        }
    }

    @Getter
    public static class HintUsedEvent extends StubEvent {
        private final AnswerHint hint;

        public HintUsedEvent(AnswerHint hint) {
            this.hint = hint;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
