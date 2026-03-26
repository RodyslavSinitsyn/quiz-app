package org.rsinitsyn.quiz.page.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestResultComponent;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.QuizComponents;

import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Route(value = "cleverest/results", layout = MainLayout.class)
@PageTitle("Cleverest - Результаты")
@PermitAll
@Slf4j
public class CleverestResultsPage extends VerticalLayout
        implements HasUrlParameter<String>, BeforeEnterObserver {

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;

    private String gameId;
    private boolean gameHost;

    public CleverestResultsPage(GameService gameService,
                                CleverestBroadcaster broadcaster) {
        this.gameService = gameService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void setParameter(BeforeEvent event, String gameId) {
        this.gameId = gameId;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (gameService.findById(gameId) == null) {
            QuizComponents.infoNotification("Игра не существует");
            event.forwardTo("");
            return;
        }
        if (broadcaster.getState(gameId) == null) {
            QuizComponents.infoNotification("Состояние игры не найдено");
            event.forwardTo("");
            return;
        }
        final var  gameEntity = gameService.findById(gameId);
        // Если игра ещё не закончилась — редиректим на /game
        if (gameEntity.getStatus() == GameStatus.STARTED) {
            event.forwardTo(CleverestGamePage.class, gameId);
            return;
        }
        if (gameEntity.getStatus() == GameStatus.NOT_STARTED) {
            event.forwardTo(CleverestWaitingPage.class, gameId);
            return;
        }
        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (gameId == null) return;

        final var state = broadcaster.getState(gameId);
        final var  resultComponent = new CleverestResultComponent();
        resultComponent.setState(
                state.getAllUserStates(),
                state.getHistory(),
                gameHost ? "" : getLoggedUser()
        );

        final var  newGameButton = new Button("Новая игра",
                e -> attachEvent.getUI().navigate(CleverestSetupPage.class));
        newGameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        add(resultComponent, newGameButton);
    }
}
