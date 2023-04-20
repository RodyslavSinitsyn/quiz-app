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
import org.rsinitsyn.quiz.model.QuizQuestionModel;

public class QuizGameAnswersComponent extends VerticalLayout {

    private List<QuizQuestionModel.QuizAnswerModel> copiedAnswerList;
    private QuizQuestionModel question;

    private ListBox<QuizQuestionModel.QuizAnswerModel> answerListBox = new ListBox<>();
    private MultiSelectListBox<QuizQuestionModel.QuizAnswerModel> multiAnswerListBox = new MultiSelectListBox<>();

    public QuizGameAnswersComponent(QuizQuestionModel question) {
        this.question = question;
        this.copiedAnswerList = new ArrayList<>(question.getShuffledAnswers());
        renderAnswers();
        setAlignItems(Alignment.STRETCH);
    }

    private void renderAnswers() {
        removeAll();
        answerListBox = new ListBox<>();
        multiAnswerListBox = new MultiSelectListBox<>();

        if (question.getType().equals(QuestionType.TEXT)) {
            answerListBox.setItems(copiedAnswerList);
            answerListBox.setRenderer(
                    new ComponentRenderer<Component, QuizQuestionModel.QuizAnswerModel>(this::createAnswerButton));
            answerListBox.addValueChangeListener(event -> fireEvent(new AnswerChoosenEvent(this, Collections.singleton(event.getValue()))));
            add(answerListBox);
        } else if (question.getType().equals(QuestionType.MULTI)) {
            multiAnswerListBox.setItems(copiedAnswerList);
            multiAnswerListBox.setRenderer(
                    new ComponentRenderer<Component, QuizQuestionModel.QuizAnswerModel>(this::createAnswerButton));

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
                        var answerModel = new QuizQuestionModel.QuizAnswerModel();
                        answerModel.setText(String.valueOf(numberField.getValue().intValue()));
                        fireEvent(new AnswerChoosenEvent(this, Collections.singleton(answerModel)));
                    });
            add(numberField, submitButton);
        }
    }

    private Button createAnswerButton(QuizQuestionModel.QuizAnswerModel answer) {
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
        private Set<QuizQuestionModel.QuizAnswerModel> answers;

        public AnswerChoosenEvent(QuizGameAnswersComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public AnswerChoosenEvent(QuizGameAnswersComponent source, Set<QuizQuestionModel.QuizAnswerModel> answers) {
            this(source, false);
            this.answers = answers;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
