package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Optional;
import java.util.UUID;

import lombok.Getter;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel.AnswerBindingModel;

import static java.util.Optional.ofNullable;

public class AnswerField extends CustomField<AnswerBindingModel> {

    private UUID id;
    private Checkbox correctOption = new Checkbox(false);
    private TextField text = new TextField("Вариант");
    @Getter
    private int index;

    public AnswerField(AnswerBindingModel answer) {
        this.id = answer.getId();
        this.index = answer.getIndex();
        configure();
        setPresentationValue(answer);
        add(correctOption, text);
    }

    private void configure() {
        setWidthFull();
        correctOption.setWidth("5%");
        text.setWidth("95%");
    }

    @Override
    protected AnswerBindingModel generateModelValue() {
        return new AnswerBindingModel(
                id,
                correctOption.getValue(),
                text.getValue(),
                index
        );
    }

    @Override
    protected void setPresentationValue(AnswerBindingModel answer) {
        ofNullable(answer)
                .ifPresent(a -> {
                    correctOption.setValue(a.isCorrect());
                    text.setValue(a.getText());
                    text.setLabel("Вариант " + (answer.getIndex() + 1));
                });
    }
}
