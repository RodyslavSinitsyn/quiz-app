package org.rsinitsyn.quiz.component.quiz;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;

public class QuizGameAnswersComponent extends VerticalLayout {

    private List<QuestionModel.AnswerModel> copiedAnswerList;
    private QuestionModel question;

    private ListBox<QuestionModel.AnswerModel> answerListBox = new ListBox<>();
    private MultiSelectListBox<QuestionModel.AnswerModel> multiAnswerListBox = new MultiSelectListBox<>();

    public QuizGameAnswersComponent(QuestionModel question) {
        this.question = question;
        this.copiedAnswerList = new ArrayList<>(question.getShuffledAnswers());
        renderAnswers();
        setAlignItems(Alignment.STRETCH);
    }

    private void renderAnswers() {
        removeAll();
        answerListBox = new ListBox<>();
        multiAnswerListBox = new MultiSelectListBox<>();

        if (question.getType().equals(QuestionType.TEXT) || question.getType().equals(QuestionType.OR)) {// todo change styles for OR
            answerListBox.setItems(copiedAnswerList);
            answerListBox.setRenderer(
                    new ComponentRenderer<Component, QuestionModel.AnswerModel>(this::createAnswerButton));
            answerListBox.addValueChangeListener(event -> fireEvent(new AnswerChoosenEvent(this, Collections.singleton(event.getValue()))));
            add(answerListBox);
        } else if (question.getType().equals(QuestionType.MULTI)) {
            multiAnswerListBox.setItems(copiedAnswerList);
            multiAnswerListBox.setRenderer(
                    new ComponentRenderer<Component, QuestionModel.AnswerModel>(this::createAnswerButton));

            var submitButton = CleverestComponents.primaryButton(
                    "Подтвердить ответ",
                    event -> fireEvent(new AnswerChoosenEvent(this, multiAnswerListBox.getSelectedItems())));
            add(multiAnswerListBox, submitButton);
        } else if (question.getType().equals(QuestionType.PRECISION)) {
            NumberField numberField = new NumberField();
            numberField.setLabel("Погрешность: +-" + question.getValidRange());

            var submitButton = CleverestComponents.primaryButton(
                    "Подтвердить ответ",
                    event -> {
                        var answerModel = new QuestionModel.AnswerModel();
                        answerModel.setText(String.valueOf(numberField.getValue().intValue()));
                        fireEvent(new AnswerChoosenEvent(this, Collections.singleton(answerModel)));
                    });
            add(numberField, submitButton);
        }
    }

    private Button createAnswerButton(QuestionModel.AnswerModel answer) {
        var button = new Button(answer.getText());
        button.addClassNames(LumoUtility.FontSize.XLARGE,
                LumoUtility.FontWeight.BOLD);
        button.setSizeFull();
        return button;
    }

    public void removeWrongAnswersAndRerender(int answersToRemove) {
        copiedAnswerList.removeAll(copiedAnswerList.stream()
                .filter(answerModel -> !answerModel.isCorrect())
                .limit(answersToRemove)
                .toList());
        renderAnswers();
    }

    @Getter
    public static class AnswerChoosenEvent extends ComponentEvent<QuizGameAnswersComponent> {
        private Set<QuestionModel.AnswerModel> answers;

        public AnswerChoosenEvent(QuizGameAnswersComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public AnswerChoosenEvent(QuizGameAnswersComponent source, Set<QuestionModel.AnswerModel> answers) {
            this(source, false);
            this.answers = answers;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
