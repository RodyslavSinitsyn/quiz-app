package org.rsinitsyn.quiz.page.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestWaitingRoomComponent;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.AllUsersReadyEvent;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.rsinitsyn.quiz.entity.GameStatus.*;
import static org.rsinitsyn.quiz.utils.QuizUtils.logState;
import static org.rsinitsyn.quiz.utils.QuizUtils.runActionInUi;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Route(value = "cleverest/waiting", layout = MainLayout.class)
@PageTitle("Cleverest - Ожидание")
@PermitAll
@Slf4j
public class CleverestWaitingPage extends VerticalLayout
        implements HasUrlParameter<String>, BeforeEnterObserver {

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;

    private String gameId;
    private boolean gameHost;
    private final List<Registration> subscriptions = new ArrayList<>();

    public CleverestWaitingPage(GameService gameService,
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
        logState(this, event.getUI(), "beforeEnter", true, subscriptions);
        // Проверяем что игра существует и state создан
        if (!validateGame(event)) {
            return;
        }
        final var gameEntity = gameService.findById(gameId);
        // Если игра уже началась — редиректим на /game
        if (gameEntity.getStatus() == STARTED) {
            event.forwardTo(CleverestGamePage.class, gameId);
            return;
        }
        // Если игра уже закончилась — редиректим на /results
        if (gameEntity.getStatus() == FINISHED) {
            event.forwardTo(CleverestResultsPage.class, gameId);
            return;
        }
        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
        logState(this, event.getUI(), "beforeEnter", false, subscriptions);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", true, subscriptions);
        if (gameId == null) return;
        final var ui = attachEvent.getUI();

        final var waitingRoom =
                new CleverestWaitingRoomComponent(gameId, broadcaster, gameHost, ui);
        add(waitingRoom);

        // Когда все игроки готовы — хост нажимает старт, все идут на /game
        subscriptions.add(broadcaster.subscribe(gameId, AllUsersReadyEvent.class, event -> {
            if (gameHost) {
                gameService.updateStatus(gameId, STARTED);
                gameService.linkQuestionsAndUsersWithGame(
                        gameId,
                        event.getUsernames(),
                        Stream.concat(
                                broadcaster.getState(gameId).getFirstQuestions().stream(),
                                broadcaster.getState(gameId).getSecondQuestions().stream()
                        ).toList());
            }
            runActionInUi(Optional.of(ui),
                    () -> ui.navigate(CleverestGamePage.class, gameId));
        }));
        logState(this, attachEvent.getUI(), "onAttach", false, subscriptions);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        logState(this, detachEvent.getUI(), "onDetach", true, subscriptions);
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
        logState(this, detachEvent.getUI(), "onDetach", false, subscriptions);
    }

    private boolean validateGame(BeforeEnterEvent event) {
        if (gameService.findById(gameId) == null) {
            QuizComponents.infoNotification("Игра не существует");
            event.forwardTo("");
            return false;
        }
        if (broadcaster.getState(gameId) == null) {
            QuizComponents.infoNotification("Состояние игры не найдено");
            event.forwardTo("");
            return false;
        }
        return true;
    }
}
