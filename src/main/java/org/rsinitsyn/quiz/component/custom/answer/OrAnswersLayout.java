package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.Collections;

public class OrAnswersLayout extends AbstractAnswersLayout {

    private final RadioButtonGroup<QuestionModel.AnswerModel> radioButtonGroup = new RadioButtonGroup<>();

    public OrAnswersLayout(QuestionModel question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        radioButtonGroup.setItems(copiedAnswerList);
        radioButtonGroup.setRenderer(
                new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                        am -> CleverestComponents.optionComponent(am.getText(), 50, event -> {
                        })));
        radioButtonGroup.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(radioButtonGroup);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        var userAnswer = radioButtonGroup.getValue();
        fireEvent(new AnswerChosenEvent(Collections.singleton(userAnswer.getText()), userAnswer.isCorrect()));
    }
}
