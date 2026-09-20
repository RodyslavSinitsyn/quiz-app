package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.entity.UserAnswerDetails;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.List;
import java.util.Set;

import static org.rsinitsyn.quiz.model.answer.AnswerResult.oneOptionResult;

public class OrAnswersLayout extends AbstractAnswersLayout {

    private final RadioButtonGroup<QuestionModel.AnswerModel> radioButtonGroup = new RadioButtonGroup<>();

    public OrAnswersLayout(AnswerLayoutRequest question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        radioButtonGroup.setItems(answers);
        radioButtonGroup.setRenderer(
                new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                        am -> CleverestComponents.optionComponent(am.text(), 30, event -> {
                        })));
        radioButtonGroup.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(radioButtonGroup);
    }

    @Override
    protected AnswerGivenEvent createAnswerGivenEvent() {
        var userAnswer = radioButtonGroup.getValue();
        return AnswerGivenEvent.builder()
                .answerDetails(UserAnswerDetails.from(
                        List.of(userAnswer.text()),
                        List.of(userAnswer.id())
                ))
                .result(oneOptionResult(userAnswer.correct()))
                .build();
    }
}
