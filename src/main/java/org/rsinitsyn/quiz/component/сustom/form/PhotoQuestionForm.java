package org.rsinitsyn.quiz.component.сustom.form;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.binding.PhotoQuestionBindingModel;
import org.rsinitsyn.quiz.utils.QuizComponents;

public class PhotoQuestionForm extends AbstractQuestionCreationForm<PhotoQuestionBindingModel> {

    private VerticalLayout layout = new VerticalLayout();

    private TextField correctOption = new TextField("Вариант А (верный)");
    private TextField optionTwo = new TextField("Вариант B");
    private TextField optionThree = new TextField("Вариант C");
    private TextField optionFour = new TextField("Вариант D");

    private final Binder<PhotoQuestionBindingModel> binder =
            new BeanValidationBinder<>(PhotoQuestionBindingModel.class);

    public PhotoQuestionForm() {
        binder.bindInstanceFields(this);

        layout.setPadding(false);
        correctOption.setWidthFull();
        optionTwo.setWidthFull();
        optionThree.setWidthFull();
        optionFour.setWidthFull();

        add(text);
        add(layout);
        addCommonComponents();
        add(createButtonsLayout());
    }

    @Override
    protected Binder<PhotoQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        binder.writeBean(model);
    }

    @Override
    protected Consumer<PhotoQuestionBindingModel> afterModelSetAction() {
        return model -> {
            layout.removeAll();
            if (model == null || StringUtils.isBlank(model.getCorrectOption())) {
                layout.add(correctOption, optionTwo, optionThree, optionFour);
            } else {
                layout.add(
                        QuizComponents.avatar(model.getCorrectOption()),
                        QuizComponents.avatar(model.getOptionTwo()),
                        QuizComponents.avatar(model.getOptionThree()),
                        QuizComponents.avatar(model.getOptionFour())
                );
            }
        };
    }
}
