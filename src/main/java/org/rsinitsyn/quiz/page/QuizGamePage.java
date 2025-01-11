package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import java.util.UUID;

import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.quiz.QuizGamePlayBoardComponent;
import org.rsinitsyn.quiz.component.quiz.QuizGameResultComponent;
import org.rsinitsyn.quiz.component.quiz.QuizGameSettingsComponent;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;

@Slf4j
@Route(value = "/quiz", layout = MainLayout.class)
@PageTitle("Game")
@PreserveOnRefresh
@PermitAll
public class QuizGamePage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;
    private QuizGameState gameState;

    private QuizGameSettingsComponent settingsComponent;
    private QuizGamePlayBoardComponent playBoardComponent;
    private QuizGameResultComponent resulComponent;

    private QuestionService questionService;
    private GameService gameService;
    private UserService userService;

    public QuizGamePage(QuestionService questionService, GameService gameService, UserService userService) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.userService = userService;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        this.gameId = parameter;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.isRefreshEvent()) {
            return;
        }
        removeAll();
        if (StringUtils.isEmpty(gameId)) {
            configureGameSettingsComponent();
            return;
        }
        if (gameService.findById(gameId) == null) {
            getUI().ifPresent(ui -> ui.navigateToClient("/"));
            return;
        }
        configurePlayGameComponent(gameState);
    }

    private void configureGameSettingsComponent() {
        settingsComponent = new QuizGameSettingsComponent(
                questionService.findAllByCurrentUserAsModel(),
                userService.findAllExceptCurrent());

        settingsComponent.addListener(QuizGameSettingsComponent.StartGameEvent.class, event -> {
            var newGameId = UUID.randomUUID().toString();
            this.gameState = event.getGameState();
            gameService.createIfNotExists(newGameId, GameType.QUIZ);
            gameService.linkQuestionsWithGame(newGameId, event.getGameState());
            getUI().ifPresent(ui -> ui.navigate(getClass(), newGameId));
        });

        add(settingsComponent);
    }

    private void configurePlayGameComponent(QuizGameState quizGameState) {
        playBoardComponent = new QuizGamePlayBoardComponent(quizGameState);
        playBoardComponent.addListener(QuizGamePlayBoardComponent.FinishGameEvent.class, event -> {
            gameService.finishGame(gameId);
            remove(playBoardComponent);
            configureQuizGameResultComponent(event.getModel());
        });
        playBoardComponent.addListener(QuizGamePlayBoardComponent.SubmitAnswerEvent.class, event -> {
            gameService.submitAnswers(
                    gameId,
                    quizGameState.getPlayerName(),
                    event.getQuestion(),
                    event.getAnswer().stream().map(QuestionModel.AnswerModel::getText).toList(),
                    () -> event.getQuestion().areAnswersCorrect(event.getAnswer()));
        });
        add(playBoardComponent);
    }

    private void configureQuizGameResultComponent(QuizGameState model) {
        resulComponent = new QuizGameResultComponent(model, gameService.findById(gameId));
        add(resulComponent);
    }
}
