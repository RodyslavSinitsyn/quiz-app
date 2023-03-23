package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;

public class QuestionForm extends FormLayout {
    FourAnswersQuestionBindingModel model = new FourAnswersQuestionBindingModel();
    TextField text = new TextField("Question text");
    TextField correctAnswerText = new TextField("Correct answer text");
    TextField secondOptionAnswerText = new TextField("Answer text 2");
    TextField thirdOptionAnswerText = new TextField("Answer text 3");
    TextField fourthOptionAnswerText = new TextField("Answer text 4");

    Binder<FourAnswersQuestionBindingModel> questionBinder = new BeanValidationBinder<>(FourAnswersQuestionBindingModel.class);

    Button save = new Button("Save");
    Button delete = new Button("Delete");
    Button close = new Button("Cancel");

    public QuestionForm() {
        questionBinder.bindInstanceFields(this);
        add(text,
                correctAnswerText,
                secondOptionAnswerText,
                thirdOptionAnswerText,
                fourthOptionAnswerText,
                createButtonsLayout());
    }

    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, model)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        return new HorizontalLayout(save, delete, close);
    }


    private void validateAndSave() {
        try {
            questionBinder.writeBean(model);
            fireEvent(new SaveEvent(this, model));
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    public void setQuestion(FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
        this.model = fourAnswersQuestionBindingModel;
        questionBinder.readBean(fourAnswersQuestionBindingModel);
    }

    public static abstract class QuestionFormEvent extends ComponentEvent<QuestionForm> {
        private FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel;

        protected QuestionFormEvent(QuestionForm source, FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
            super(source, false);
            this.fourAnswersQuestionBindingModel = fourAnswersQuestionBindingModel;
        }

        public FourAnswersQuestionBindingModel getQuestion() {
            return fourAnswersQuestionBindingModel;
        }
    }

    public static class SaveEvent extends QuestionFormEvent {
        SaveEvent(QuestionForm source, FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
            super(source, fourAnswersQuestionBindingModel);
        }
    }

    public static class DeleteEvent extends QuestionFormEvent {
        DeleteEvent(QuestionForm source, FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
            super(source, fourAnswersQuestionBindingModel);
        }

    }

    public static class CloseEvent extends QuestionFormEvent {
        CloseEvent(QuestionForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
