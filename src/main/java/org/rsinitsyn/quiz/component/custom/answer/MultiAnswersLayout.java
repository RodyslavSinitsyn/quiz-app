package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.AnswerHint;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MultiAnswersLayout extends AbstractAnswersLayout {

    private final MultiSelectListBox<QuestionModel.AnswerModel> multiAnswerListBox = new MultiSelectListBox<>();

    public MultiAnswersLayout(QuestionModel question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        multiAnswerListBox.setItems(copiedAnswerList);
        multiAnswerListBox.setRenderer(
                new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                        am -> CleverestComponents.optionComponent(am.getText(), 50, event -> {
                        })));
        multiAnswerListBox.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(multiAnswerListBox);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        var userAnswers = multiAnswerListBox.getSelectedItems();
        long correctAnswersCount = question.getAnswers().stream().filter(QuestionModel.AnswerModel::isCorrect).count();
        long userCorrectAnswersCount = userAnswers.stream().filter(QuestionModel.AnswerModel::isCorrect).count();
        boolean userHasOnlyCorrectAnswers = userCorrectAnswersCount == userAnswers.size();
        boolean isCorrect = userHasOnlyCorrectAnswers && correctAnswersCount == userCorrectAnswersCount;

        fireEvent(new AnswerChosenEvent(multiAnswerListBox.getSelectedItems().stream()
                .map(QuestionModel.AnswerModel::getText)
                .collect(Collectors.toSet()),
                isCorrect));
    }

    @Override
    protected List<Component> getHintsComponents() {
        Button revealCountHint = new Button("Количество ответов");
        revealCountHint.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        revealCountHint.setEnabled(hintsState.getHintsUsage().get(AnswerHint.CORRECT_COUNT));
        revealCountHint.addClickListener(event -> {
            revealCountHint.setText("Верных ответов: "
                    + question.getAnswers().stream().filter(QuestionModel.AnswerModel::isCorrect).count());
            revealCountHint.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            fireEvent(new HintUsedEvent(AnswerHint.CORRECT_COUNT));
        });
        return Collections.singletonList(revealCountHint);
    }
}
