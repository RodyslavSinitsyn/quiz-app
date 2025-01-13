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
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.SessionWrapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Route(value = "/quiz/game", layout = MainLayout.class)
@PageTitle("Game")
@PreserveOnRefresh
@PermitAll
public class QuizGamePlayPage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;
    private QuizGameState gameState;

    private QuizGamePlayBoardComponent playBoardComponent;
    private QuizGameResultComponent resulComponent;

    private GameService gameService;

    private List<Registration> subscriptions = new ArrayList<>();

    public QuizGamePlayPage(GameService gameService) {
        this.gameService = gameService;
        this.gameState = SessionWrapper.getQuizGameState();
        renderComponents();
    }

    private void renderComponents() {
        configurePlayGameComponent();
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.isRefreshEvent()) {
            return;
        }
        if (gameService.findById(gameId) == null) {
            getUI().ifPresent(ui -> ui.navigateToClient("/"));
        }
    }

    private void configurePlayGameComponent() {
        playBoardComponent = new QuizGamePlayBoardComponent(gameState);
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
