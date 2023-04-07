package org.rsinitsyn.quiz.component.quiz;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

public class QuizGameAnswersComponent extends VerticalLayout {

    private List<QuizQuestionModel.QuizAnswerModel> copiedAnswerList;
    private QuestionType questionType;

    private ListBox<QuizQuestionModel.QuizAnswerModel> answerListBox = new ListBox<>();
    private MultiSelectListBox<QuizQuestionModel.QuizAnswerModel> multiAnswerListBox = new MultiSelectListBox<>();

    public QuizGameAnswersComponent(List<QuizQuestionModel.QuizAnswerModel> answerList,
                                    QuestionType questionType) {
        this.copiedAnswerList = answerList;
        this.questionType = questionType;
        renderAnswers();
        setAlignItems(Alignment.STRETCH);
    }

    private void renderAnswers() {
        removeAll();
        answerListBox = new ListBox<>();
        multiAnswerListBox = new MultiSelectListBox<>();

        if (questionType.equals(QuestionType.TEXT)) {
            answerListBox.setItems(copiedAnswerList);
            answerListBox.setRenderer(
                    new ComponentRenderer<Component, QuizQuestionModel.QuizAnswerModel>(this::createAnswerButton));
            answerListBox.addValueChangeListener(event -> fireEvent(new AnswerChoosenEvent(this, Collections.singleton(event.getValue()))));
            add(answerListBox);
        } else if (questionType.equals(QuestionType.MULTI)) {
            multiAnswerListBox.setItems(copiedAnswerList);
            multiAnswerListBox.setRenderer(
                    new ComponentRenderer<Component, QuizQuestionModel.QuizAnswerModel>(this::createAnswerButton));

            var submitButton = new Button();
            submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            submitButton.setText("Подтвердить ответ");
            submitButton.addClickListener(event -> fireEvent(new AnswerChoosenEvent(this, multiAnswerListBox.getSelectedItems())));
            add(multiAnswerListBox, submitButton);
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