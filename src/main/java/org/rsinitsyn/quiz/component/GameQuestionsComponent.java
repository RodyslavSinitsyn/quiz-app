package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.Optional;
import org.rsinitsyn.quiz.entity.GameEntity;

public class GameQuestionsComponent extends VerticalLayout {

    public GameQuestionsComponent(GameEntity gameEntity, Component rowSeparator) {
        configure(gameEntity, rowSeparator);
    }

    public GameQuestionsComponent(GameEntity gameEntity) {
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

            column.add(answerSpan, text);
            add(column);
            Optional.ofNullable(rowSeparator).ifPresent(this::add);
        });
    }
}