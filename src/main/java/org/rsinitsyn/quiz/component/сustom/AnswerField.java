package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Optional;
import java.util.UUID;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;

public class AnswerField extends CustomField<FourAnswersQuestionBindingModel.AnswerBindingModel> {

    private UUID id;
    private Checkbox correctOption = new Checkbox(false);
    private TextField text = new TextField("Вариант");
    private int index;

    public AnswerField(FourAnswersQuestionBindingModel.AnswerBindingModel answer) {
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


    public int getIndex() {
        return index;
    }

    @Override
    protected FourAnswersQuestionBindingModel.AnswerBindingModel generateModelValue() {
        return new FourAnswersQuestionBindingModel.AnswerBindingModel(
                id,
                correctOption.getValue(),
                text.getValue(),
                index
        );
    }

    @Override
    protected void setPresentationValue(FourAnswersQuestionBindingModel.AnswerBindingModel answer) {
        Optional.ofNullable(answer)
                .ifPresent(a -> {
                    correctOption.setValue(a.isCorrect());
                    text.setValue(a.getText());
                    text.setLabel("Вариант " + (answer.getIndex() + 1));
                });
    }
}
