package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.AnswerHint;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.Collections;
import java.util.List;

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
        options.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(options);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        fireEvent(new AnswerChosenEvent(Collections.singleton(options.getValue().getText()),
                options.getValue().isCorrect()));
    }

    @Override
    protected List<Component> getHintsComponents() {
        Button halfHint = new Button("50 на 50");
        halfHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        halfHint.addClickListener(event -> {
            removeWrongAnswersAndRerender(2);
            hintsLayout.setEnabled(false);
            fireEvent(new HintUsedEvent(AnswerHint.HALF));
        });
        halfHint.setEnabled(!hintsState.getHintsUsage().get(AnswerHint.HALF));

        Button threeLeftHint = new Button("3/4");
        threeLeftHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        threeLeftHint.addClickListener(event -> {
            removeWrongAnswersAndRerender(1);
            hintsLayout.setEnabled(false);
            fireEvent(new HintUsedEvent(AnswerHint.THREE));
        });
        threeLeftHint.setEnabled(!hintsState.getHintsUsage().get(AnswerHint.THREE));

        return List.of(halfHint, threeLeftHint);
    }
}
