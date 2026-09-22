package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.page.cleverest.CleverestGamePage;
import org.rsinitsyn.quiz.page.cleverest.CleverestResultsPage;
import org.rsinitsyn.quiz.page.cleverest.CleverestWaitingPage;
import org.rsinitsyn.quiz.page.quiz.QuizGamePlayPage;
import org.rsinitsyn.quiz.view.GameDetailsView;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.rsinitsyn.quiz.entity.GameStatus.*;
import static org.rsinitsyn.quiz.utils.QuizUtils.DATE_FORMAT_VALUE;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

public class GameListGrid extends Grid<GameDetailsView> {

    private final Function<UUID, GameEntity> gameEntityProvider;

    public GameListGrid(final List<GameDetailsView> gameList,
                        final Function<UUID, GameEntity> gameEntityProvider) {
        this.gameEntityProvider = gameEntityProvider;
        setItems(gameList);
        configureGrid();
    }

    private void configureGrid() {
        addColumn(new ComponentRenderer<>(this::getStatusComponent)).setHeader("Статус");
        addColumn(GameDetailsView::getCreatedBy).setHeader("Создатель");
        addColumn(GameDetailsView::getType).setHeader("Тип");
        addColumn(GameDetailsView::getName).setHeader("Название");
        addColumn(GameDetailsView::playerNames).setHeader("Игроки");
        addColumn(GameDetailsView::questionsResult).setHeader("Вопросы");
        addColumn(GameDetailsView::resultPercentage).setHeader("Результаты");
        addColumn(new LocalDateTimeRenderer<>(GameDetailsView::getCreationDate, DATE_FORMAT_VALUE)).setHeader("Создана");
        addColumn(new LocalDateTimeRenderer<>(GameDetailsView::getFinishDate, DATE_FORMAT_VALUE)).setHeader("Окончена");
        setAllRowsVisible(true);
        setDetailsVisibleOnClick(true);
        setItemDetailsRenderer(new ComponentRenderer<>(gameView ->
                new GameResultsComponent(gameEntityProvider.apply(gameView.getId()))));
    }

    private Component getStatusComponent(GameDetailsView game) {
        if (game.getStatus().equals(FINISHED)) {
            return new Span("Закончена");
        } else if (game.getStatus().equals(STARTED)
                && game.playerNames().contains(getLoggedUser())) {
            return joinGameButton("Вернуться", game, game.getType());
        } else if (game.getStatus().equals(STARTED)) {
            return new Span("Начата");
        } else if (game.getStatus().equals(NOT_STARTED)) {
            return joinGameButton("Зайти", game, game.getType());
        } else {
            return new Span("Не настроена");
        }
    }

    private Button joinGameButton(String label, GameDetailsView GAME, GameType gameType) {
        final var gameId = GAME.getId().toString();
        var button = new Button(label);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> {
            event.getSource().getUI().ifPresent(ui -> {
                if (gameType == GameType.CLEVEREST) {
                    switch (GAME.getStatus()) {
                        case NOT_STARTED -> ui.navigate(CleverestWaitingPage.class, gameId);
                        case STARTED -> ui.navigate(CleverestGamePage.class, gameId);
                        case FINISHED -> ui.navigate(CleverestResultsPage.class, gameId);
                    }
                } else if (gameType == GameType.QUIZ) {
                    ui.navigate(QuizGamePlayPage.class, gameId);
                }
            });
        });
        return button;
    }
}
