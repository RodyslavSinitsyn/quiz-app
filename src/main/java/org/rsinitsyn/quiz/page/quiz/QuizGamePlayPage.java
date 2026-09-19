package org.rsinitsyn.quiz.page.quiz;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.quiz.QuizGamePlayBoardComponent;
import org.rsinitsyn.quiz.component.quiz.QuizGameResultComponent;
import org.rsinitsyn.quiz.entity.UserAnswerDetails;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.service.GameQuestionUserAnswerService;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.GameStateService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.SessionWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.rsinitsyn.quiz.entity.GameStatus.*;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUserId;

@Slf4j
@Route(value = "/quiz/game", layout = MainLayout.class)
@PageTitle("Game")
@PreserveOnRefresh
@PermitAll
public class QuizGamePlayPage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private UUID gameId;
    private QuizGameState gameState;

    private QuizGamePlayBoardComponent playBoardComponent = new QuizGamePlayBoardComponent();
    private QuizGameResultComponent resulComponent;

    private GameService gameService;
    private GameStateService gameStateService;
    private GameQuestionUserAnswerService gameQuestionUserAnswerService;

    private List<Registration> subscriptions = new ArrayList<>();

    public QuizGamePlayPage(GameService gameService,
                            GameStateService gameStateService,
                            GameQuestionUserAnswerService gameQuestionUserAnswerService) {
        this.gameService = gameService;
        this.gameStateService = gameStateService;
        this.gameQuestionUserAnswerService = gameQuestionUserAnswerService;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.isRefreshEvent()) {
            return;
        }
        if (!gameService.exists(gameId)) {
            getUI().ifPresent(ui -> {
                ui.navigateToClient("/");
                QuizComponents.infoNotification("Game not found");
            });
            return;
        }
//        this.gameState = gameService.restoreQuizGameState(gameId.toString());
        this.gameState = gameStateService.restoreQuizGameStateNew(gameId, getLoggedUserId());
        if (!SessionWrapper.getLoggedUser().equals(gameState.getPlayerName())) {
            getUI().ifPresent(ui -> {
                ui.navigateToClient("/");
                QuizComponents.infoNotification("Not your game");
            });
            return;
        }
        if (gameState.getStatus() == FINISHED) {
            configureQuizGameResultComponent();
            return;
        }
        if (gameState.getStatus() == NOT_STARTED) {
            gameService.updateStatus(gameId.toString(), STARTED);
        }
        configurePlayGameComponent();
    }

    private void configurePlayGameComponent() {
        playBoardComponent.setState(gameState);
        add(playBoardComponent);
    }

    private void configureQuizGameResultComponent() {
        resulComponent = new QuizGameResultComponent(gameState, gameService.findById(gameId.toString()));
        add(resulComponent);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.gameId = UUID.fromString(parameter);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscriptions.add(playBoardComponent.addGameFinishedEventListener(event -> {
            gameService.finishGame(gameId.toString());
            remove(playBoardComponent);
            configureQuizGameResultComponent();
        }));
        subscriptions.add(playBoardComponent.addSubmitUserAnswerEventListener(event -> {
            // old submit
            gameService.submitAnswers(
                    gameId.toString(),
                    gameState.getPlayerName(),
                    event.getQuestion(),
                    event.getAnswers().stream().toList(),
                    event::getAnswerStatus);
            // new submit
            gameQuestionUserAnswerService.submitAnswer(
                    gameId,
                    event.getQuestion().getId(),
                    gameState.getPlayerId(),
                    UserAnswerDetails.from(event.getAnswers().stream().toList()),
                    event.getAnswerStatus(),
                    null // todo: response time can be null but passing of params looks ugly
            );
        }));
        log.trace("onAttach. subscribe {}", subscriptions.size());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        log.trace("onDetach. unsubscribe {}", subscriptions.size());
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }
}
