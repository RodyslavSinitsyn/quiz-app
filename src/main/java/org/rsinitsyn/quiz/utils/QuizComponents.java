package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import java.util.function.BiConsumer;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;

@UtilityClass
public class QuizComponents {

    public Span questionMechanicSpan(QuestionEntity question) {
        return questionMechanicSpan(question.isOptionsOnly(), question.getType());
    }

    public Span questionMechanicSpan(boolean optionsOnly, QuestionType questionType) {
        Span result = new Span();
        if (optionsOnly) {
            result.getElement().getThemeList().add("badge contrast");
        } else {
            result.getElement().getThemeList().add("badge");
        }
        Icon optionsOnlyIcon = questionType.equals(QuestionType.MULTI)
                ? VaadinIcon.LIST_OL.create()
                : VaadinIcon.PUZZLE_PIECE.create();
        result.add(optionsOnlyIcon);
        return result;
    }

    public Upload uploadComponent(String uploadLabel, BiConsumer<MemoryBuffer, SucceededEvent> eventHandler, String allowedTypes) {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setUploadButton(new Button(uploadLabel));
        upload.setDropAllowed(true);
        upload.setDropLabel(new Label(""));
        upload.setAcceptedFileTypes(allowedTypes);
        upload.addSucceededListener(event -> eventHandler.accept(buffer, event));
        return upload;
    }
}
