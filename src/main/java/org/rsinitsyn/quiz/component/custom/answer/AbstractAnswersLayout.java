package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import lombok.Builder;
import lombok.Getter;
import org.rsinitsyn.quiz.component.custom.event.StubEvent;
import org.rsinitsyn.quiz.model.AnswerHint;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.HintsState;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.submitButton;

public abstract class AbstractAnswersLayout extends VerticalLayout {

    protected final QuestionModel question;
    protected final List<QuestionModel.AnswerModel> answers;
    protected HintsState hintsState;

    // Components
    protected final HorizontalLayout hintsLayout = new HorizontalLayout();
    protected final Button submitButton = submitButton(e -> {
    });

    public AbstractAnswersLayout(AnswerLayoutRequest request) {
        this.question = request.getQuestion();
        this.answers = new ArrayList<>(request.getQuestion().getShuffledAnswers());
        this.hintsState = request.getHintsState();
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
        submitButton.addClickListener(e -> fireEvent(createAnswerGivenEvent()));
        submitButton.setEnabled(false);
        add(submitButton);
    }

    protected abstract void renderAnswers();

    protected abstract AnswerGivenEvent createAnswerGivenEvent();

    protected List<Component> getHintsComponents() {
        return Collections.emptyList();
    }

    protected void renderHintsLayout() {
        hintsLayout.removeAll();
        var hintComponents = getHintsComponents();
        if (!hintsState.hintsEnabled() || hintComponents.isEmpty()) {
            hintsLayout.setVisible(false);
            return;
        }
        hintsLayout.setAlignItems(Alignment.CENTER);
        hintsLayout.add(getHintsComponents());
        add(hintsLayout);
    }

    protected void removeWrongAnswersAndRerender(int answersToRemove) {
        answers.removeAll(answers.stream()
                .filter(answerModel -> !answerModel.correct())
                .limit(answersToRemove)
                .toList());
        renderAnswers();
    }

    @Getter
    @Builder
    public static class AnswerGivenEvent extends StubEvent {
        private final Set<String> answers;
        protected final AnswerResult result;
        @Builder.Default
        private final boolean manuallyApprove = false;

        public boolean isCorrect() {
            return result.status().correct();
        }
    }

    @Getter
    public static class HintUsedEvent extends StubEvent {
        private final AnswerHint hint;

        public HintUsedEvent(AnswerHint hint) {
            this.hint = hint;
        }
    }

    public Registration addAnswerGivenListener(ComponentEventListener<AnswerGivenEvent> listener) {
        return getEventBus().addListener(AnswerGivenEvent.class, listener);
    }

    public Registration addHintUsedListener(ComponentEventListener<HintUsedEvent> listener) {
        return getEventBus().addListener(HintUsedEvent.class, listener);
    }
}
