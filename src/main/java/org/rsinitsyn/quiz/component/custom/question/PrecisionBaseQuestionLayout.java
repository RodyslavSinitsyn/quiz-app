package org.rsinitsyn.quiz.component.custom.question;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;

public class PrecisionBaseQuestionLayout extends BaseQuestionLayout {

    public PrecisionBaseQuestionLayout(QuestionLayoutRequest request) {
        super(request);
    }

    @Override
    protected void renderQuestionText() {
        add(new Span(VaadinIcon.STAR.create(), getQuestionTextElement()));
    }
}
