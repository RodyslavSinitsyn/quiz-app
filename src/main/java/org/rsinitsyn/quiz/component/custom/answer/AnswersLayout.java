package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.Collections;
import java.util.List;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.optionComponent;
import static org.rsinitsyn.quiz.model.AnswerHint.*;

public class AnswersLayout extends AbstractAnswersLayout {
    private final ListBox<QuestionModel.AnswerModel> options = new ListBox<>();

    public AnswersLayout(AnswerLayoutRequest request) {
        super(request);
    }

    @Override
    protected void renderAnswers() {
        options.setItems(answers);
        options.setRenderer(
                new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                        am -> optionComponent(am.text(), 50, event -> {
                        })));
        options.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(options);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        fireEvent(new AnswerChosenEvent(Collections.singleton(options.getValue().text()),
                options.getValue().correct()));
    }

    @Override
    protected List<Component> getHintsComponents() {
        final var halfHint = new Button("50 на 50");
        halfHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        halfHint.addClickListener(event -> {
            removeWrongAnswersAndRerender(2);
            hintsLayout.setEnabled(false);
            fireEvent(new HintUsedEvent(HALF));
        });
        halfHint.setEnabled(!hintsState.hintsUsage().get(HALF));

        final var threeLeftHint = new Button("3/4");
        threeLeftHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        threeLeftHint.addClickListener(event -> {
            removeWrongAnswersAndRerender(1);
            hintsLayout.setEnabled(false);
            fireEvent(new HintUsedEvent(THREE));
        });
        threeLeftHint.setEnabled(!hintsState.hintsUsage().get(THREE));

        return List.of(halfHint, threeLeftHint);
    }
}
