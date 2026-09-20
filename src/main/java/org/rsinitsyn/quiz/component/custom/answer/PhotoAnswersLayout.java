package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.rsinitsyn.quiz.entity.UserAnswerDetails;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.List;
import java.util.Set;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.SMALL_IMAGE_HEIGHT;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.image;
import static org.rsinitsyn.quiz.model.answer.AnswerResult.oneOptionResult;

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
                answerModel -> image(answerModel.photoFilename(), SMALL_IMAGE_HEIGHT)));
        options.addValueChangeListener(e -> submitButton.setEnabled(true));
        add(options);
    }

    @Override
    protected AnswerGivenEvent createAnswerGivenEvent() {
        var userAnswer = options.getValue();
        return AnswerGivenEvent.builder()
                .answerDetails(UserAnswerDetails.from(
                        List.of("%s - %s".formatted(userAnswer.text(), userAnswer.photoFilename())),
                        List.of(userAnswer.id())
                ))
                .result(oneOptionResult(userAnswer.correct()))
                .build();
    }
}
