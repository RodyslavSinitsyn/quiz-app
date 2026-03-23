package org.rsinitsyn.quiz.component.custom.form;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.List;
import java.util.function.Consumer;

import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.model.binding.TopQuestionBindingModel;

public class TopQuestionForm extends AbstractQuestionCreationForm<TopQuestionBindingModel> {

    private final Binder<TopQuestionBindingModel> binder =
            new BeanValidationBinder<>(TopQuestionBindingModel.class);

    public TopQuestionForm(List<QuestionCategoryEntity> categories) {
        super(categories);
        binder.bindInstanceFields(this);

        final var topListText = new TextArea("Топ список");
        topListText.setRequired(true);

        final var sequence = new Checkbox("Установить хронологию", false);

        add(text, topListText, sequence);

        addCommonComponents();
    }

    @Override
    protected Binder<TopQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        binder.writeBean(model);
    }

    @Override
    protected Consumer<TopQuestionBindingModel> afterModelSetAction() {
        return null;
    }
}
