package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.textfield.NumberField;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;

import java.util.Collections;

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
        boolean isCorrect = false;
        if (StringUtils.isNumeric(answerText)) {
            int userAnswerNumeric = Integer.parseInt(answerText);
            int validAnswerNumeric = Integer.parseInt(question.getAnswers().stream().findFirst().orElseThrow().text());
            isCorrect = Math.abs(validAnswerNumeric - userAnswerNumeric) <= question.getValidRange();
        }
        return new AnswerGivenEvent(Collections.singleton(answerText), isCorrect);
    }
}
