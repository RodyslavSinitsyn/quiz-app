package org.rsinitsyn.quiz.component.custom.form;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.model.binding.LinkQuestionBindingModel;

import java.util.List;
import java.util.function.Consumer;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.horizontalLayoutBetween;

public class LinkQuestionForm extends AbstractQuestionCreationForm<LinkQuestionBindingModel> {

    private final TextArea leftAnswers = new TextArea("Левый столбец"){{
        setWidthFull();
    }};
    private final TextArea rightAnswers = new TextArea("Правый столбец"){{
        setWidthFull();
    }};

    private final Binder<LinkQuestionBindingModel> binder =
            new BeanValidationBinder<>(LinkQuestionBindingModel.class);

    public LinkQuestionForm(List<QuestionCategoryEntity> categories) {
        super(categories);
        binder.bindInstanceFields(this);
        add(text, horizontalLayoutBetween(leftAnswers, rightAnswers));
        addCommonComponents();
    }

    @Override
    protected Binder<LinkQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        if (leftAnswers.getValue().lines().count() != rightAnswers.getValue().lines().count()) {
            throw new IllegalStateException("Колво линий не совпадает");
        }
        binder.writeBean(model);
    }

    @Override
    protected Consumer<LinkQuestionBindingModel> afterModelSetAction() {
        return null;
    }
}
