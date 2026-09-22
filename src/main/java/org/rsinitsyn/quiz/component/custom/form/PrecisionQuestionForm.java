package org.rsinitsyn.quiz.component.custom.form;

import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.List;
import java.util.function.Consumer;

import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;

public class PrecisionQuestionForm extends AbstractQuestionCreationForm<PrecisionQuestionBindingModel> {

    private final NumberField answerText = new NumberField("Ответ");
    private final NumberField range = new NumberField("Допустимая погрешность");

    private final Binder<PrecisionQuestionBindingModel> binder =
            new BeanValidationBinder<>(PrecisionQuestionBindingModel.class);

    public PrecisionQuestionForm(List<QuestionCategoryEntity> categories) {
        super(categories);
        binder.bindInstanceFields(this);

        add(text, answerText, range);
        addCommonComponents();
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


