package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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
}
