package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.Collections;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;

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
    protected void submitHandler(ClickEvent<Button> event) {
        var userAnswer = options.getValue();
        fireEvent(new AnswerChosenEvent(Collections.singleton(userAnswer.text()), userAnswer.correct()));
    }
}
