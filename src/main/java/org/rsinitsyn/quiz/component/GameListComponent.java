package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.List;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class GameListComponent extends VerticalLayout {

    private Grid<GameEntity> grid = new Grid<>();

    private List<GameEntity> gameList;

    public GameListComponent(List<GameEntity> gameList) {
        this.gameList = gameList;
        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.setItems(gameList);
        grid.addColumn(new LocalDateTimeRenderer<>(GameEntity::getFinishDate, QuizUtils.DATE_FORMAT_VALUE)).setHeader("Дата");
        grid.addColumn(GameEntity::getName).setHeader("Название");
        grid.addColumn(GameEntity::getPlayerName).setHeader("Игрок");
        grid.addColumn(gameEntity -> gameEntity.getResult() + "%").setHeader("Результат");
        grid.setDetailsVisibleOnClick(true);
        grid.setItemDetailsRenderer(getQuestionsDetailsRenderer());
        grid.setAllRowsVisible(true);
    }

    private ComponentRenderer<VerticalLayout, GameEntity> getQuestionsDetailsRenderer() {
        return new ComponentRenderer<>(gameEntity -> {
            VerticalLayout rows = new VerticalLayout();
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
                rows.add(column);
            });
            return rows;
        });
    }

}
