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
        radioButtonGroup.setRenderer(
                new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                        am -> CleverestComponents.optionComponent(am.getText(), 50, event -> {
                        })));

        add(radioButtonGroup);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        var userAnswer = radioButtonGroup.getOptionalValue().orElseThrow();
        fireEvent(new AnswerChosenEvent<>(this, Collections.singleton(userAnswer), userAnswer.isCorrect()));
    }

    @Override
    protected boolean isSubmitButtonEnabled() {
        return radioButtonGroup.getOptionalValue().isPresent();
    }
}
