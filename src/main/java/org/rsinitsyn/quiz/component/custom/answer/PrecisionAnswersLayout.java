package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.NumberField;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;

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
    protected void submitHandler(ClickEvent<Button> event) {
        var userAnswer = new QuestionModel.AnswerModel();
        userAnswer.setText(String.valueOf(numberField.getValue().intValue()));
        boolean isCorrect = false;
        if (StringUtils.isNumeric(userAnswer.getText())) {
            int userAnswerNumeric = Integer.parseInt(userAnswer.getText());
            int validAnswerNumeric = Integer.parseInt(question.getAnswers().stream().findFirst().orElseThrow().getText());
            isCorrect = Math.abs(validAnswerNumeric - userAnswerNumeric) <= question.getValidRange();
        }
        fireEvent(new AnswerChosenEvent(Collections.singleton(userAnswer.getText()), isCorrect));
    }
}
