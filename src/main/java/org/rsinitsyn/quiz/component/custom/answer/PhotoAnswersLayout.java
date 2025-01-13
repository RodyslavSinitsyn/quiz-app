package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.Collections;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.SMALL_IMAGE_HEIGHT;

public class PhotoAnswersLayout extends AbstractAnswersLayout {

    private final ListBox<QuestionModel.AnswerModel> options = new ListBox<>();

    public PhotoAnswersLayout(QuestionModel question) {
        super(question);
    }

    @Override
    protected void renderAnswers() {
        options.setItems(copiedAnswerList);
        options.setWidthFull();
        options.setRenderer(new ComponentRenderer<Component, QuestionModel.AnswerModel>(
                answerModel -> {
                    Image image = new Image();
                    image.setSrc(QuizUtils.createStreamResourceForPhoto(answerModel.getPhotoFilename()));
                    image.setMaxHeight(SMALL_IMAGE_HEIGHT);
                    image.setWidthFull();
                    image.getStyle().set("object-fit", "cover");
                    image.getStyle().set("object-position", "center center");
                    image.addClassNames(
                            LumoUtility.AlignSelf.CENTER,
                            LumoUtility.Border.ALL,
                            LumoUtility.BorderRadius.MEDIUM,
                            LumoUtility.BorderColor.PRIMARY
                    );
                    return image;
                }));
        add(options);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        var userAnswer = options.getValue();
        fireEvent(new AnswerChosenEvent<>(this, Collections.singleton(userAnswer), userAnswer.isCorrect()));
    }

    @Override
    protected boolean isSubmitButtonEnabled() {
        return options.getValue() != null;
    }
}
