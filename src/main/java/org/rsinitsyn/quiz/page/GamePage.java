package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.QuizGamePlayBoardComponent;
import org.rsinitsyn.quiz.component.QuizGameSettingsComponent;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "/game", layout = MainLayout.class)
@PageTitle("Game")
@Slf4j
public class GamePage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;

    private QuizGameSettingsComponent quizGameSettingsComponent;
    private QuizGamePlayBoardComponent quizGamePlayBoardComponent;

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
            log.info("Refresh page happened");
            return;
        }
        configureGameSettingsComponent();
        add(quizGameSettingsComponent);
        createGameIfNotExists();
    }

    private QuizGamePlayBoardComponent configurePlayGameComponent(QuizGameStateModel quizGameStateModel) {
        quizGamePlayBoardComponent = new QuizGamePlayBoardComponent(quizGameStateModel);
        quizGamePlayBoardComponent.addListener(QuizGamePlayBoardComponent.FinishGameEvent.class, event -> {
            gameService.finish(gameId, event.getModel());
        });
        return quizGamePlayBoardComponent;
    }

    private void configureGameSettingsComponent() {
        quizGameSettingsComponent = new QuizGameSettingsComponent(
                ModelConverterUtils.toQuizQuestionModels(questionDao.findAll())
        );

        quizGameSettingsComponent.addListener(QuizGameSettingsComponent.StartGameEvent.class, event -> {
            remove(quizGameSettingsComponent);
            add(configurePlayGameComponent(event.getModel()));
            gameService.update(gameId,
                    event.getModel().getGameName(),
                    event.getModel().getPlayerName(),
                    GameStatus.STARTED,
                    event.getModel().getQuestions().size(),
                    null);
        });

        quizGameSettingsComponent.addListener(QuizGameSettingsComponent.UpdateGameEvent.class, event -> {
            gameService.update(gameId,
                    event.getModel().getGameName(),
                    event.getModel().getPlayerName(),
                    GameStatus.NOT_STARTED,
                    event.getModel().getQuestions().size(),
                    null);
        });
    }

    private void createGameIfNotExists() {
        gameService.createIfNotExists(gameId);
    }
}
