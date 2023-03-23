package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.GameSettingsComponent;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.PlayGameComponent;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.model.GameStateModel;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.springframework.beans.factory.annotation.Autowired;

@PreserveOnRefresh
@Route(value = "/game", layout = MainLayout.class)
@PageTitle("Game")
@Slf4j
public class GamePage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;

    private GameSettingsComponent gameSettingsComponent;
    private PlayGameComponent playGameComponent;

    private QuestionDao questionDao;
    private GameService gameService;

    @Autowired
    public GamePage(QuestionDao questionDao, GameService gameService) {
        this.questionDao = questionDao;
        this.gameService = gameService;
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        gameId = parameter;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.isRefreshEvent()) {
            log.debug("Refresh page happened");
            return;
        }
        configureGameSettingsComponent();
        add(gameSettingsComponent);
        createGameIfNotExists();
    }

    private PlayGameComponent configurePlayGameComponent(GameStateModel gameStateModel) {
        playGameComponent = new PlayGameComponent(gameStateModel);
        playGameComponent.addListener(PlayGameComponent.FinishGameEvent.class, event -> {
            gameService.finish(gameId, event.getModel());
        });
        return playGameComponent;
    }

    private void configureGameSettingsComponent() {
        gameSettingsComponent = new GameSettingsComponent(
                ModelConverterUtils.toQuestionModels(questionDao.findAll()));

        gameSettingsComponent.addListener(GameSettingsComponent.StartGameEvent.class, event -> {
            remove(gameSettingsComponent);
            add(configurePlayGameComponent(event.getModel()));
            gameService.update(gameId,
                    event.getModel().getGameName(),
                    event.getModel().getPlayerName(),
                    GameStatus.STARTED,
                    event.getModel().getQuestions().size(),
                    null);
        });

        gameSettingsComponent.addListener(GameSettingsComponent.UpdateGameEvent.class, event -> {
            gameService.update(gameId,
                    event.getModel().getGameName(),
                    event.getModel().getPlayerName(),
                    GameStatus.NOT_STARTED,
                    event.getModel().getQuestions().size(),
                    null);
        });
    }

    private void createGameIfNotExists() {
        gameService.create(gameId);
    }
}
