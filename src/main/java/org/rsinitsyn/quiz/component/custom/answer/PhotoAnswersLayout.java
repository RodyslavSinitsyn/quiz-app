package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.Collections;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.*;

public class PhotoAnswersLayout extends AbstractAnswersLayout {

    private final ListBox<QuestionModel.AnswerModel> options = new ListBox<>();

    public PhotoAnswersLayout(AnswerLayoutRequest question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        options.setItems(answers);
        options.setWidthFull();
        options.setRenderer(new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                answerModel -> image(answerModel.photoFilename(), MEDIUM_IMAGE_HEIGHT)));
        options.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(options);
    }

    @Override
    protected AnswerGivenEvent createAnswerGivenEvent() {
        var userAnswer = options.getValue();
        return new AnswerGivenEvent(Collections.singleton(userAnswer.text()), userAnswer.correct());
    }
}
