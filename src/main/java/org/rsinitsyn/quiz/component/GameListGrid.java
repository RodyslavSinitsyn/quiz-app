package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.List;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class GameListGrid extends Grid<GameEntity> {

    public GameListGrid(List<GameEntity> gameList) {
        setItems(gameList);
        configureGrid();
    }

    private void configureGrid() {
        addColumn(new LocalDateTimeRenderer<>(GameEntity::getFinishDate, QuizUtils.DATE_FORMAT_VALUE)).setHeader("Дата");
        addColumn(GameEntity::getName).setHeader("Название");
        addColumn(GameEntity::getPlayerNames).setHeader("Игрок");
        addColumn(gameEntity -> {
            int totalSize = gameEntity.getGameQuestions().size();
            long correctSize = gameEntity.getGameQuestions().stream().filter(GameQuestionUserEntity::getAnswered).count();
            return correctSize + "/" + totalSize;
        }).setHeader("Вопросов");
        addColumn(gameEntity -> "???%").setHeader("Результат");
        setAllRowsVisible(true);
    }
}
