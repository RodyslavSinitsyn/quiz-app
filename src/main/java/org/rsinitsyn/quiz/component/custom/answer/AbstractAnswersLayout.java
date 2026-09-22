package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.RangeInput;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.rsinitsyn.quiz.component.custom.event.StubEvent;
import org.rsinitsyn.quiz.entity.UserAnswerDetails;
import org.rsinitsyn.quiz.model.AnswerHint;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.HintsState;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;
import org.rsinitsyn.quiz.model.answer.AnswerBet;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.horizontalLayoutBetween;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.submitButton;

public abstract class AbstractAnswersLayout extends VerticalLayout {

    protected final QuestionModel question;
    protected final Optional<String> username;
    protected final List<AnswerModel> answers;
    protected HintsState hintsState;
    protected Optional<AnswerBet> answerBet;

    // Components
    protected final HorizontalLayout hintsLayout = new HorizontalLayout();
    protected final Button submitButton = submitButton(e -> {
    });

    public AbstractAnswersLayout(AnswerLayoutRequest request) {
        this.question = request.getQuestion();
        this.username = request.getUsername();
        this.answers = questionAnswers(request);
        this.hintsState = request.getHintsState();
        this.answerBet = request.getAnswerBet();
        setAlignItems(Alignment.STRETCH);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderComponents();
    }

    private void renderComponents() {
        renderHintsLayout();
        renderBets();
        renderAnswers();
        renderSubmitButton();
    }

    protected List<AnswerModel> questionAnswers(AnswerLayoutRequest request) {
        return new ArrayList<>(request.getQuestion().getShuffledAnswers());
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
        hintsLayout.add(hintComponents);
        add(hintsLayout);
    }

    protected void renderBets() {
        if (answerBet.isEmpty()) {
            return;
        }
        final var bet = answerBet.get();

        final var slider = new RangeInput();
        slider.setWidthFull();
        slider.setMax(bet.min());
        slider.setMax(bet.max());
        slider.setStep(1.0);

        final var labelText = new Span("+1 | -1");

        slider.addValueChangeListener(event -> {
            final var multiplier = event.getValue().intValue();
            labelText.setText("+%d | -%d".formatted(multiplier, multiplier));
            fireEvent(new BetChangedEvent(username.orElseThrow(), multiplier));
        });

        add(horizontalLayoutBetween(labelText, slider));
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
        private final UserAnswerDetails answerDetails;
        private final AnswerResult result;
        @Builder.Default
        private final boolean manuallyApprove = false;

        public List<String> getTextAnswers() {
            return answerDetails.answerTexts();
        }
    }

    @Getter
    public static class HintUsedEvent extends StubEvent {
        private final AnswerHint hint;

        public HintUsedEvent(AnswerHint hint) {
            this.hint = hint;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class InputChangedEvent extends StubEvent {
        private final String username;
        private final String text;

        public InputChangedEvent(final String username, final String text) {
            this.username = username;
            this.text = text;
        }
    }

    @Getter
    @Accessors(fluent = true)
    public static class BetChangedEvent extends StubEvent {
        private final String username;
        private final int value;

        public BetChangedEvent(final String username, final int value) {
            this.username = username;
            this.value = value;
        }
    }

    public Registration addAnswerGivenListener(ComponentEventListener<AnswerGivenEvent> listener) {
        return getEventBus().addListener(AnswerGivenEvent.class, listener);
    }

    public Registration addHintUsedListener(ComponentEventListener<HintUsedEvent> listener) {
        return getEventBus().addListener(HintUsedEvent.class, listener);
    }

    public Registration addInputChangedListener(ComponentEventListener<InputChangedEvent> listener) {
        return getEventBus().addListener(InputChangedEvent.class, listener);
    }

    public Registration addBetChangedListener(ComponentEventListener<BetChangedEvent> listener) {
        return getEventBus().addListener(BetChangedEvent.class, listener);
    }
}
