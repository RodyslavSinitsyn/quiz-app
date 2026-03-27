package org.rsinitsyn.quiz.page.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestGamePlayBoardComponent;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestGamePlayBoardComponentOld;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.rsinitsyn.quiz.utils.QuizComponents.infoNotification;
import static org.rsinitsyn.quiz.utils.QuizUtils.logState;
import static org.rsinitsyn.quiz.utils.QuizUtils.runActionInUi;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;
import static org.rsinitsyn.quiz.utils.ThemeUtils.BLACK_COLOR;
import static org.rsinitsyn.quiz.utils.ThemeUtils.restoreTheme;

@Route(value = "cleverest/game", layout = MainLayout.class)
@PageTitle("Cleverest - Игра")
@PermitAll
@Slf4j
@PreserveOnRefresh
public class CleverestGamePage extends VerticalLayout
        implements HasUrlParameter<String>, BeforeEnterObserver, BeforeLeaveObserver {

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;
    private final QuestionService questionService;

    private String gameId;
    private boolean gameHost;
    private String originalLocation;
    private final List<Registration> subscriptions = new ArrayList<>();

    public CleverestGamePage(GameService gameService,
                             CleverestBroadcaster broadcaster,
                             QuestionService questionService) {
        this.gameService = gameService;
        this.broadcaster = broadcaster;
        this.questionService = questionService;
    }

    @Override
    public void setParameter(BeforeEvent event, String gameId) {
        this.gameId = gameId;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        logState(this, event.getUI(), "beforeEnter", true, subscriptions);
        if (!validateGame(event)) {
            return;
        }
        restoreTheme();
        final var gameEntity = gameService.findById(gameId);

        if (gameEntity.getStatus() == GameStatus.NOT_STARTED) {
            event.forwardTo(CleverestWaitingPage.class, gameId);
            return;
        }
        if (gameEntity.getStatus() == GameStatus.FINISHED) {
            event.forwardTo(CleverestResultsPage.class, gameId);
            return;
        }

        if (!broadcaster.stateExists(gameId)) {
            broadcaster.createState(gameId,
                    gameEntity.getCreatedBy(),
                    gameEntity.getGameQuestions().stream()
                            .map(gq -> gq.getQuestion())
                            .map(questionService::toQuizQuestionModel)
                            .toList(),
                    List.of(),
                    List.of());
            gameEntity.getGameQuestions().stream()
                    .map(GameQuestionUserEntity::getUser)
                    .filter(u -> !u.getUsername().equals(gameEntity.getCreatedBy()))
                    .forEach(u -> broadcaster.sendJoinUserEvent(
                            gameId,
                            u.getUsername(),
                            BLACK_COLOR,
                            null,
                            null,
                            null
                    ));
        }

        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
        final var state = broadcaster.getState(gameId);

        if (!gameHost && !state.userPresent(getLoggedUser())) {
            infoNotification("Игра уже началась, вы там не участвуете");
            event.forwardTo("");
            return;
        }

        originalLocation = event.getLocation().getPathWithQueryParameters();
        logState(this, event.getUI(), "beforeEnter", false, subscriptions);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", true, subscriptions);
        if (gameId == null) {
            return;
        }
        final var ui = attachEvent.getUI();

        // При рефреше страница уже содержит children от предыдущего attach
        boolean isRefresh = !getChildren().findAny().isEmpty();
        removeAll();

        final var playBoard = new CleverestGamePlayBoardComponent();
        playBoard.setState(gameId, broadcaster, gameHost, isRefresh, ui);
        add(playBoard);

        if (gameHost) {
            subscriptions.add(broadcaster.subscribe(gameId,
                    CleverestBroadcaster.SaveUsersAnswersEvent.class,
                    event -> gameService.submitAnswersBatch(
                            gameId, event.getQuestion(), event.getUserStateSnapshots())));

            subscriptions.add(broadcaster.subscribe(gameId,
                    CleverestBroadcaster.GameFinishedEvent.class,
                    event -> {
                        gameService.finishGame(gameId);
                        QuizUtils.runActionInUi(Optional.of(ui),
                                () -> ui.navigate(CleverestResultsPage.class, gameId));
                    }));

            playBoard.addUpdateQuestionGradeEventListener(event ->
                    questionService.updateQuestionGrade(
                            event.question().getId(),
                            event.username(),
                            event.grade()));
        } else {
            subscriptions.add(broadcaster.subscribe(gameId,
                    CleverestBroadcaster.GameFinishedEvent.class,
                    event -> QuizUtils.runActionInUi(Optional.of(ui),
                            () -> ui.navigate(CleverestResultsPage.class, gameId))));
        }
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
    public void beforeLeave(BeforeLeaveEvent event) {
        if (gameId == null || broadcaster.getState(gameId) == null) {
            return;
        }
        final var gameEntity = gameService.findById(gameId);
        if (gameEntity == null || gameEntity.getStatus() != GameStatus.STARTED) {
            return;
        }
        final var confirmDialog = new Dialog();
        confirmDialog.setHeaderTitle("Нельзя покинуть игру!");
        confirmDialog.setCloseOnOutsideClick(true);
        confirmDialog.addDialogCloseActionListener(e -> confirmDialog.close());
        confirmDialog.open();
        event.postpone();
        event.getUI().getPage().getHistory().replaceState(null, originalLocation);
    }

    private boolean validateGame(BeforeEnterEvent event) {
        if (!gameService.exist(gameId)) {
            runActionInUi(event.getUI(), () -> infoNotification("Игра не существует"));
            event.forwardTo("");
            return false;
        }
//        if (broadcaster.getState(gameId) == null) {
//            infoNotification("Состояние игры не найдено");
//            event.forwardTo("");
//            return false;
//        }
        return true;
    }
}
