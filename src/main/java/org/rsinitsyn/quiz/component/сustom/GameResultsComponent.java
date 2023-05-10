package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.utils.QuizComponents;

public class GameResultsComponent extends VerticalLayout {

    public GameResultsComponent(GameEntity gameEntity, Component rowSeparator) {
        configure(gameEntity, rowSeparator);
    }

    public GameResultsComponent(GameEntity gameEntity) {
        configure(gameEntity, null);
    }

    private void configure(GameEntity gameEntity, Component rowSeparator) {
        if (gameEntity == null || gameEntity.getGameQuestions().isEmpty()) {
            add(new Span("Нет игры или же у игры нет вопросов"));
            return;
        }
        gameEntity.getGameQuestions().forEach(question -> {
            HorizontalLayout column = new HorizontalLayout();

            Span answerIcon;
            if (question.getAnswered()) {
                answerIcon = new Span(VaadinIcon.CHECK.create());
                answerIcon.getElement().getThemeList().add("badge success");
            } else {
                answerIcon = new Span(VaadinIcon.CLOSE_SMALL.create());
                answerIcon.getElement().getThemeList().add("badge error");
            }
            Span categoryName = new Span(question.getQuestion().getCategory().getName());
            categoryName.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.LIGHT);

            Span userName = new Span(" / " + question.getUser().getUsername());

            Span userAnswer = new Span(
                    StringUtils.defaultIfEmpty(question.getAnswerText(), ""));
            userAnswer.addClassNames(LumoUtility.FontWeight.SEMIBOLD);

            column.add(answerIcon,
                    categoryName,
                    QuizComponents.questionDescription(question.getQuestion()),
                    userAnswer,
                    userName);
            add(column);
            Optional.ofNullable(rowSeparator).ifPresent(this::add);
        });
    }
}
