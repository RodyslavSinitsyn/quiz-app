package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;

public class PrecisionQuestionForm extends FormLayout {

    @Getter
    private PrecisionQuestionBindingModel model = new PrecisionQuestionBindingModel();

    private TextArea text = new TextArea("Вопрос");
    private NumberField answerText = new NumberField("Ответ");
    private NumberField range = new NumberField("Допустимая погрешность");

    private Button button = new Button("Создать");

    private Binder<PrecisionQuestionBindingModel> binder = new BeanValidationBinder<>(PrecisionQuestionBindingModel.class);

    public PrecisionQuestionForm() {
        setWidth("30em");
        binder.bindInstanceFields(this);

        button.addClickListener(event -> {
            validate();
            fireEvent(new SaveEvent(this));
        });

        add(text, answerText, range, button);
    }

    @SneakyThrows
    private void validate() {
        binder.writeBean(model);
    }

    public void setModel(PrecisionQuestionBindingModel model) {
        this.model = model;
        this.binder.readBean(model);
    }

    @Getter
    public static class SaveEvent extends ComponentEvent<PrecisionQuestionForm> {
        public SaveEvent(PrecisionQuestionForm source) {
            super(source, false);
        }
    }

    @Getter
    public static class CancelEvent extends ComponentEvent<PrecisionQuestionForm> {
        public CancelEvent(PrecisionQuestionForm source) {
            super(source, false);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}


