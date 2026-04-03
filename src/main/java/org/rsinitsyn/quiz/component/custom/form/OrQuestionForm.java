package org.rsinitsyn.quiz.component.custom.form;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.List;
import java.util.function.Consumer;

import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.model.binding.OrQuestionBindingModel;

public class OrQuestionForm extends AbstractQuestionCreationForm<OrQuestionBindingModel> {

    private TextField correctAnswerText = new TextField("Верный ответ");
    private TextField optionAnswerText = new TextField("Вариант ответа");

    private final Binder<OrQuestionBindingModel> binder =
            new BeanValidationBinder<>(OrQuestionBindingModel.class);


    public OrQuestionForm(List<QuestionCategoryEntity> categories) {
        super(categories);
        binder.bindInstanceFields(this);

        correctAnswerText.setRequired(true);
        optionAnswerText.setRequired(true);

        add(text, correctAnswerText, optionAnswerText);
        addCommonComponents();
    }

    @Override
    protected Binder<OrQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        binder.writeBean(model);
    }

    @Override
    protected Consumer<OrQuestionBindingModel> afterModelSetAction() {
        return null;
    }
}
