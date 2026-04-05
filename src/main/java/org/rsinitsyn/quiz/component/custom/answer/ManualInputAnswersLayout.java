package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.textfield.TextField;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

import java.util.Set;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.textAnswerInput;

public class ManualInputAnswersLayout extends AbstractAnswersLayout {

    private final TextField answerField = textAnswerInput((e) -> {
        submitButton.setEnabled(!e.getValue().isBlank());
    });

    public ManualInputAnswersLayout(final AnswerLayoutRequest request) {
        super(request);
    }

    @Override
    protected void renderAnswers() {
        answerField.addValueChangeListener(e ->
                fireEvent(new InputChangedEvent(username.orElseThrow(), e.getValue())));
        add(answerField);
    }

    @Override
    protected AnswerGivenEvent createAnswerGivenEvent() {
        return AnswerGivenEvent.builder()
                .answers(Set.of(answerField.getValue()))
                .result(new AnswerResult(AnswerStatus.UNKNOWN, question.getPoints(), 0))
                .manuallyApprove(true)
                .build();
    }
}
