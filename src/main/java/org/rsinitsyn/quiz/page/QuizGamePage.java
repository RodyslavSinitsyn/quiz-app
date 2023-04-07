package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.quiz.QuizGamePlayBoardComponent;
import org.rsinitsyn.quiz.component.quiz.QuizGameResultComponent;
import org.rsinitsyn.quiz.component.quiz.QuizGameSettingsComponent;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "/quiz", layout = MainLayout.class)
@PageTitle("Game")
@Slf4j
public class QuizGamePage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;

    private QuizGameSettingsComponent quizGameSettingsComponent;
    private QuizGamePlayBoardComponent quizGamePlayBoardComponent;
    private QuizGameResultComponent quizGameResultComponent;

    private QuestionService questionService;
    private GameService gameService;
    private UserService userService;

    @Autowired
    public QuizGamePage(QuestionService questionService, GameService gameService, UserService userService) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.userService = userService;
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
            gameService.finishQuizGame(gameId, event.getModel());
            remove(quizGamePlayBoardComponent);
            add(configureQuizGameResultComponent(event.getModel()));
        });
        quizGamePlayBoardComponent.addListener(QuizGamePlayBoardComponent.SubmitAnswerEvent.class, event -> {
            gameService.submitAnswers(
                    gameId,
                    quizGameStateModel.getPlayerName(),
                    event.getQuestion(),
                    event.getAnswer());
        });
        return quizGamePlayBoardComponent;
    }

    private QuizGameResultComponent configureQuizGameResultComponent(QuizGameStateModel model) {
        quizGameResultComponent = new QuizGameResultComponent(model, gameService.findById(gameId));
        return quizGameResultComponent;
    }

    private void configureGameSettingsComponent() {
        List<UserEntity> playerList = userService.findAllExceptCurrent();
        quizGameSettingsComponent = new QuizGameSettingsComponent(
                gameId,
                questionService.findAllByCurrentUserAsModel(),
                playerList);

        quizGameSettingsComponent.addListener(QuizGameSettingsComponent.StartGameEvent.class, event -> {
            gameService.linkQuestionsWithGame(gameId, event.getModel());
            remove(quizGameSettingsComponent);
            add(configurePlayGameComponent(event.getModel()));
        });

        quizGameSettingsComponent.addListener(QuizGameSettingsComponent.UpdateGameEvent.class, event -> {
            gameService.update(gameId,
                    event.getModel().getGameName(),
                    QuizUtils.getLoggedUser(),
                    GameStatus.NOT_STARTED,
                    event.getModel().getQuestions().size(),
                    null);
        });
    }

    private void createGameIfNotExists() {
        boolean gameCreated = gameService.createIfNotExists(gameId, GameType.QUIZ);
        if (!gameCreated) {
            Notification notification =
                    Notification.show("Нет возможности продолжить созданную игру. Создайте новую игру!",
                            3_000,
                            Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            removeAll();
            getUI().ifPresent(ui -> ui.navigate(QuizGamePage.class, UUID.randomUUID().toString()));
        }
    }
}
