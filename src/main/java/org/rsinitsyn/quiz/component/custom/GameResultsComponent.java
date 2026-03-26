package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.QuestionType;

import java.util.Optional;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.horizontalLayoutBetween;
import static org.rsinitsyn.quiz.utils.QuizComponents.questionDescription;
import static org.rsinitsyn.quiz.utils.QuizComponents.smallAvatar;

public class GameResultsComponent extends VerticalLayout {

    public GameResultsComponent(GameEntity gameEntity, Component rowSeparator) {
        configure(gameEntity, rowSeparator);
    }

    public GameResultsComponent(GameEntity gameEntity) {
        configure(gameEntity, null);
    }

    private void configure(GameEntity gameEntity, Component rowSeparator) {
        if (gameEntity == null || gameEntity.getGameQuestions().isEmpty()) {
            add(new Span("Игра не настроена"));
            return;
        }
        gameEntity.getGameQuestions().forEach(question -> {
            Span answerIcon;
            if (question.getAnswered() == null) {
                answerIcon = new Span(VaadinIcon.MINUS_CIRCLE_O.create());
            } else if (question.getAnswered()) {
                answerIcon = new Span(VaadinIcon.CHECK.create());
                answerIcon.getElement().getThemeList().add("badge success");
            } else {
                answerIcon = new Span(VaadinIcon.CLOSE_SMALL.create());
                answerIcon.getElement().getThemeList().add("badge error");
            }
            Span categoryName = new Span(question.getQuestion().getCategory().getName());
            categoryName.addClassNames(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.LIGHT);

            Span userName = new Span(" / " + question.getUser().getUsername());

            Span userAnswer = new Span();

            if (question.getQuestion().getType().equals(QuestionType.PHOTO)) {
                userAnswer.add(smallAvatar(question.getAnswerText()));
            } else {
                userAnswer.add(StringUtils.defaultIfEmpty(question.getAnswerText(), ""));
                userAnswer.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            }

            add(horizontalLayoutBetween(answerIcon,
                    categoryName,
                    questionDescription(question.getQuestion()),
                    userAnswer,
                    userName));
            Optional.ofNullable(rowSeparator).ifPresent(this::add);
        });
    }
}
