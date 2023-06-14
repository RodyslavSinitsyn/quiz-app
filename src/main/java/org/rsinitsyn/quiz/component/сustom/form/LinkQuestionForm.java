package org.rsinitsyn.quiz.component.сustom.form;

import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.function.Consumer;
import org.rsinitsyn.quiz.model.binding.LinkQuestionBindingModel;

public class LinkQuestionForm extends AbstractQuestionCreationForm<LinkQuestionBindingModel> {

    private final TextArea leftAnswers = new TextArea("Левый столбец");
    private final TextArea rightAnswers = new TextArea("Правый столбец");

    private final Binder<LinkQuestionBindingModel> binder =
            new BeanValidationBinder<>(LinkQuestionBindingModel.class);

    public LinkQuestionForm() {
        binder.bindInstanceFields(this);

        leftAnswers.setWidthFull();
        rightAnswers.setWidthFull();

        HorizontalLayout layout = new HorizontalLayout(leftAnswers, rightAnswers);
        layout.setWidthFull();
        layout.setPadding(false);

        layout.setAlignItems(FlexComponent.Alignment.CENTER);

        add(text, layout);
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
