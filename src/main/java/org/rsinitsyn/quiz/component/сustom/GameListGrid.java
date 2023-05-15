package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.List;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.page.CleverestGamePage;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class GameListGrid extends Grid<GameEntity> {

    public GameListGrid(List<GameEntity> gameList) {
        setItems(gameList);
        configureGrid();
    }

    private void configureGrid() {
        addColumn(new ComponentRenderer<>(gameEntity -> {
            if (gameEntity.getStatus().equals(GameStatus.FINISHED)) {
                return new Span("Закончена");
            } else if (gameEntity.getStatus().equals(GameStatus.STARTED)) {
                return new Span("Начата");
            } else if (gameEntity.getStatus().equals(GameStatus.NOT_STARTED)
                    && gameEntity.getType().equals(GameType.CLEVEREST)) {
                var button = new Button("Зайти");
                button.addClickListener(event -> {
                    event.getSource().getUI().ifPresent(ui -> {
                        ui.navigate(CleverestGamePage.class, gameEntity.getId().toString());
                    });
                });
                return button;
            } else {
                return new Span("Не настроена");
            }
        }))
                .setHeader("Статус");
        addColumn(GameEntity::getCreatedBy).setHeader("Создатель");
        addColumn(GameEntity::getType).setHeader("Тип");
        addColumn(GameEntity::getName).setHeader("Название");
        addColumn(GameEntity::getPlayerNames).setHeader("Игроки");
        addColumn(gameEntity -> {
            int totalSize = gameEntity.getGameQuestions().size();
            long correctSize = gameEntity.getGameQuestions().stream()
                    .filter(e -> e.getAnswered() != null)
                    .filter(GameQuestionUserEntity::getAnswered).count();
            return correctSize + "/" + totalSize;
        }).setHeader("Вопросы");
        addColumn(gameEntity -> {
            int totalSize = gameEntity.getGameQuestions().size();
            long correctSize = gameEntity.getGameQuestions().stream()
                    .filter(e -> e.getAnswered() != null)
                    .filter(GameQuestionUserEntity::getAnswered).count();
            return QuizUtils.divide(correctSize * 100, totalSize) + "%";
        }).setHeader("Результаты");
        addColumn(new LocalDateTimeRenderer<>(GameEntity::getCreationDate, QuizUtils.DATE_FORMAT_VALUE)).setHeader("Создана");
        addColumn(new LocalDateTimeRenderer<>(GameEntity::getFinishDate, QuizUtils.DATE_FORMAT_VALUE)).setHeader("Окончена");
        setAllRowsVisible(true);
        setDetailsVisibleOnClick(true);
        setItemDetailsRenderer(new ComponentRenderer<>(GameResultsComponent::new));
    }
}
