package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.List;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
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
        grid.addColumn(gameEntity -> {
            long correctCount = gameEntity.getGameQuestions()
                    .stream()
                    .filter(GameQuestionEntity::getAnswered)
                    .count();
            return correctCount + "/" + gameEntity.getGameQuestions().size();
        }).setHeader("Вопросов");
        grid.addColumn(gameEntity -> gameEntity.getResult() + "%").setHeader("Результат");
        grid.setDetailsVisibleOnClick(true);
        grid.setItemDetailsRenderer(new ComponentRenderer<>(GameQuestionsComponent::new));
        grid.setAllRowsVisible(true);
    }
}
