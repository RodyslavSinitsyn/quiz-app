package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import java.util.List;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class GameListComponent extends VerticalLayout {

    private TreeGrid<GameEntity> treeGrid = new TreeGrid<>();

    private List<GameEntity> gameList;

    public GameListComponent(List<GameEntity> gameList) {
        this.gameList = gameList;
        configureGrid();
        add(treeGrid);
    }

    private void configureGrid() {
        treeGrid.setAllRowsVisible(true);
        treeGrid.addHierarchyColumn(gameEntity -> QuizUtils.formatDate(gameEntity.getFinishDate())).setHeader("Дата");
        treeGrid.addColumn(GameEntity::getName).setHeader("Название");
        treeGrid.addColumn(GameEntity::getPlayerName).setHeader("Игрок");
        treeGrid.addColumn(gameEntity -> gameEntity.getResult() + "%").setHeader("Результат");

        treeGrid.setItems(gameList, this::getGameQuestions);
    }

    // Кастыль
    private List<GameEntity> getGameQuestions(GameEntity gameEntity) {
        return gameEntity.getGameQuestions()
                .stream()
                .map(question -> {
                    GameEntity entity = new GameEntity();
                    entity.setFinishDate(gameEntity.getFinishDate());
                    entity.setName(question.getQuestion().getText());
                    entity.setPlayerName(question.getAnswered() ? "Верно" : "Не верно");
                    entity.setResult(0);
                    return entity;
                })
                .toList();
    }

}
