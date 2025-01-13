package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.Collections;

public class AnswersLayout extends AbstractAnswersLayout {
    private final ListBox<QuestionModel.AnswerModel> options = new ListBox<>();

    public AnswersLayout(QuestionModel question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        options.setItems(copiedAnswerList);
        options.setRenderer(
                new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                        am -> CleverestComponents.optionComponent(am.getText(), 50, event -> {
                        })));
        add(options);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        fireEvent(new AnswerChosenEvent<>(this,
                Collections.singleton(options.getValue()),
                options.getValue().isCorrect()));
    }

    @Override
    protected boolean isSubmitButtonEnabled() {
        return options.getValue() != null;
    }

}
