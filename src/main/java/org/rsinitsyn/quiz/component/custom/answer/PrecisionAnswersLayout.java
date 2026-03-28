package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.textfield.NumberField;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

import java.util.Collections;
import java.util.Set;

import static org.rsinitsyn.quiz.entity.AnswerStatus.*;

public class PrecisionAnswersLayout extends AbstractAnswersLayout {

    private final NumberField numberField = new NumberField();

    public PrecisionAnswersLayout(AnswerLayoutRequest question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        numberField.setLabel("Погрешность: +-" + question.getValidRange());
        numberField.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(numberField);
    }

    @Override
    protected AnswerGivenEvent createAnswerGivenEvent() {
        final var answerText = String.valueOf(numberField.getValue().intValue());

        final var validAnswer = Integer.parseInt(
                question.getAnswers().stream()
                        .findFirst()
                        .orElseThrow()
                        .text()
        );

        final var userAnswer = Integer.parseInt(answerText);
        final var diff = Math.abs(validAnswer - userAnswer);

        var result = new AnswerResult(WRONG, 1, 0);
        if (diff == 0) {
            result = new AnswerResult(CORRECT, 1, 1);
        } else if (diff <= question.getValidRange()) {
            result = new AnswerResult(PARTIAL, 1, 0);
        }

        return AnswerGivenEvent.builder()
                .answers(Set.of(answerText))
                .result(result)
                .build();
    }
}
