package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.optionComponent;

public class SequenceAnswersLayout extends AbstractAnswersLayout {

    private final VerticalLayout sequenceContainer = new VerticalLayout();

    public SequenceAnswersLayout(final AnswerLayoutRequest request) {
        super(request);
    }

    @Override
    protected void renderAnswers() {
        sequenceContainer.removeAll();

        for (int i = 0; i < answers.size(); i++) {
            final var index = i;
            final var answer = answers.get(i);

            final var option = optionComponent(answer.text(), 50, event -> {});

            final var up = new Button(VaadinIcon.ARROW_CIRCLE_UP.create(), e -> moveUp(index));
            final var down = new Button(VaadinIcon.ARROW_CIRCLE_DOWN.create(), e -> moveDown(index));

            up.setEnabled(i > 0);
            down.setEnabled(i < answers.size() - 1);

            final var row = new HorizontalLayout(down, option, up);

            row.setWidthFull();
            row.setAlignItems(Alignment.CENTER);

            sequenceContainer.add(row);
        }

        if (getChildren().noneMatch(component -> component == sequenceContainer)) {
            add(sequenceContainer);
        };

        submitButton.setEnabled(true);
    }

    private void moveUp(int index) {
        Collections.swap(answers, index, index - 1);
        renderAnswers();
    }

    private void moveDown(int index) {
        Collections.swap(answers, index, index + 1);
        renderAnswers();
    }

    @Override
    protected void submitHandler(final ClickEvent<Button> event) {
        final var correct = IntStream.range(0, answers.size())
                .allMatch(i -> answers.get(i).number() == i + 1);

        final var selected = answers.stream()
                .map(AnswerModel::text)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        fireEvent(new AnswerChosenEvent(selected, correct));
    }
}
