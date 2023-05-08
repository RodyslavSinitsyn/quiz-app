package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Optional;
import org.rsinitsyn.quiz.entity.GameEntity;

public class GameQuestionsAnswersComponent extends VerticalLayout {

    public GameQuestionsAnswersComponent(GameEntity gameEntity, Component rowSeparator) {
        configure(gameEntity, rowSeparator);
    }

    public GameQuestionsAnswersComponent(GameEntity gameEntity) {
        configure(gameEntity, null);
    }

    private void configure(GameEntity gameEntity, Component rowSeparator) {
        if (gameEntity == null || gameEntity.getGameQuestions().isEmpty()) {
            add(new Span("Нет игры или же у игры нет вопросов"));
            return;
        }
        gameEntity.getGameQuestions().forEach(question -> {
            HorizontalLayout column = new HorizontalLayout();
            Span text = new Span(question.getQuestion().getText());
            Span answerSpan;
            if (question.getAnswered()) {
                answerSpan = new Span(VaadinIcon.CHECK.create());
                answerSpan.getElement().getThemeList().add("badge success");
            } else {
                answerSpan = new Span(VaadinIcon.CLOSE_SMALL.create());
                answerSpan.getElement().getThemeList().add("badge error");
            }
            Span categoryName = new Span(question.getQuestion().getCategory().getName());
            categoryName.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.EXTRALIGHT);

            column.add(answerSpan, text, categoryName);
            add(column);
            Optional.ofNullable(rowSeparator).ifPresent(this::add);
        });
    }
}
