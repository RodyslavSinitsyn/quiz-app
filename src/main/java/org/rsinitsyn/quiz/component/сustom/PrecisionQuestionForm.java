package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;

public class PrecisionQuestionForm extends AbstractQuestionCreationForm<PrecisionQuestionBindingModel> {

    private final TextArea text = new TextArea("Вопрос");
    private final NumberField answerText = new NumberField("Ответ");
    private final NumberField range = new NumberField("Допустимая погрешность");

    private final Binder<PrecisionQuestionBindingModel> binder =
            new BeanValidationBinder<>(PrecisionQuestionBindingModel.class);

    public PrecisionQuestionForm() {
        binder.bindInstanceFields(this);
        add(text, answerText, range);
        add(createButtonsLayout());
    }

    @Override
    protected Binder<PrecisionQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        binder.writeBean(model);
    }

    @Override
    protected Consumer<PrecisionQuestionBindingModel> afterModelSetAction() {
        return null;
    }
}


