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
import org.rsinitsyn.quiz.service.CleverestBroadcaster.UserJoinedEvent;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.rsinitsyn.quiz.entity.GameStatus.NOT_STARTED;
import static org.rsinitsyn.quiz.entity.GameStatus.STARTED;
import static org.rsinitsyn.quiz.utils.QuizUtils.*;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Route(value = "cleverest/waiting", layout = MainLayout.class)
@PageTitle("Cleverest - Ожидание")
@PermitAll
@Slf4j
public class CleverestWaitingPage extends VerticalLayout
        implements HasUrlParameter<String>, BeforeEnterObserver, AfterNavigationObserver {

    private final QuestionService questionService;
    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;
    private final PageValidator pageValidator;

    private String gameId;
    private boolean gameHost;
    private final List<Registration> subscriptions = new ArrayList<>();

    private CleverestWaitingRoomComponent waitingRoom;

    public CleverestWaitingPage(final QuestionService questionService,
                                final GameService gameService,
                                final CleverestBroadcaster broadcaster,
                                final PageValidator pageValidator) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.broadcaster = broadcaster;
        this.pageValidator = pageValidator;
    }

    @Override
    public void setParameter(BeforeEvent event, String gameId) {
        this.gameId = gameId;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        logState(this, event.getUI(), "beforeEnter", true, subscriptions);
        final var result = pageValidator.validate(gameId, NOT_STARTED);
        if (result.navigationRequired()) {
            result.navigateAction().ifPresent(a -> a.accept(event));
            runActionInUi(event.getUI(), () -> result.notificationMessage().ifPresent(QuizComponents::infoNotification));
            return;
        }
        final var gameEntity = result.game();
        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
        if (waitingRoom == null) {
            waitingRoom = new CleverestWaitingRoomComponent(gameHost,
                    broadcaster.getState(gameId).getAllUserProfiles(),
                    gameId);
            add(waitingRoom);
        }
        logState(this, event.getUI(), "beforeEnter", false, subscriptions);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", true, subscriptions);
        if (gameId == null) return;
        final var ui = attachEvent.getUI();

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
            runActionInUi(ui,
                    () -> ui.navigate(CleverestGamePage.class, gameId));
        }));

        subscriptions.add(broadcaster.subscribe(gameId, UserJoinedEvent.class, event ->
                runActionInUi(ui, () -> {
                    if (doneByAuthenticated(event)) {
                        waitingRoom.updateUserState(event.getUser());
                    }
                    waitingRoom.updateTableAndBets(event.getAllUsers());
                })));

        // waiting room events
        subscriptions.add(waitingRoom.addUserSubmitDataEventListener(event -> broadcaster.sendJoinUserEvent(
                gameId, event.username(), event.color(), event.photo(), event.userWinner(), event.userLoser()
        )));
        subscriptions.add(waitingRoom.addStartGameEventListener(event ->
                broadcaster.sendUsersReadyEvent(gameId)));

        logState(this, attachEvent.getUI(), "onAttach", false, subscriptions);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        logState(this, detachEvent.getUI(), "onDetach", true, subscriptions);
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
        logState(this, detachEvent.getUI(), "onDetach", false, subscriptions);
    }

    @Override
    public void afterNavigation(final AfterNavigationEvent event) {
    }
}
