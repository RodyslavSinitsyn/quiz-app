package org.rsinitsyn.quiz.page;

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
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.SessionWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Route(value = "/quiz/game", layout = MainLayout.class)
@PageTitle("Game")
@PreserveOnRefresh
@PermitAll
public class QuizGamePlayPage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;
    private QuizGameState gameState;

    private QuizGamePlayBoardComponent playBoardComponent = new QuizGamePlayBoardComponent();
    private QuizGameResultComponent resulComponent;

    private GameService gameService;

    private List<Registration> subscriptions = new ArrayList<>();

    public QuizGamePlayPage(GameService gameService) {
        this.gameService = gameService;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.isRefreshEvent()) {
            return;
        }
        if (!gameService.exists(UUID.fromString(gameId))) {
            getUI().ifPresent(ui -> {
                ui.navigateToClient("/");
                QuizComponents.infoNotification("Game not found");
            });
            return;
        }
        this.gameState = gameService.restoreQuizGameState(gameId);
        if (!SessionWrapper.getLoggedUser().equals(gameState.getPlayerName())) {
            getUI().ifPresent(ui -> {
                ui.navigateToClient("/");
                QuizComponents.infoNotification("Not your game");
            });
            return;
        }
        if (gameState.getStatus() == GameStatus.FINISHED) {
            configureQuizGameResultComponent();
            return;
        }
        if (gameState.getStatus() == GameStatus.NOT_STARTED) {
            gameService.updateStatus(gameId, GameStatus.STARTED);
        }
        configurePlayGameComponent();
    }

    private void configurePlayGameComponent() {
        playBoardComponent.setState(gameState);
        add(playBoardComponent);
    }

    private void configureQuizGameResultComponent() {
        resulComponent = new QuizGameResultComponent(gameState, gameService.findById(gameId));
        add(resulComponent);
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        this.gameId = parameter;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscriptions.add(playBoardComponent.addListener(QuizGamePlayBoardComponent.FinishGameEvent.class, event -> {
            gameService.finishGame(gameId);
            remove(playBoardComponent);
            configureQuizGameResultComponent();
        }));
        subscriptions.add(playBoardComponent.addListener(QuizGamePlayBoardComponent.SubmitUserAnswer.class, event -> {
            gameService.submitAnswers(
                    gameId,
                    gameState.getPlayerName(),
                    event.getQuestion(),
                    event.getAnswers().stream().toList(),
                    event::isCorrect);
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
