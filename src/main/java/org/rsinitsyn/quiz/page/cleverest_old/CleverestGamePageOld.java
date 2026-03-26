package org.rsinitsyn.quiz.page.cleverest_old;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestGamePlayBoardComponentOld;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestGameSettingsComponent;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestResultComponent;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestWaitingRoomComponentOld;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.AllUsersReadyEvent;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.GameFinishedEvent;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rsinitsyn.quiz.service.CleverestBroadcaster.QuestionGradedEvent;
import static org.rsinitsyn.quiz.service.CleverestBroadcaster.SaveUsersAnswersEvent;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

/*
    constructor
    setParameter
    beforeEnter
    onAttach
    afterNavigation
    beforeLeave
    onDetach
 */
@Route(value = "/cleverest-old", layout = MainLayout.class)
@PageTitle("Cleverest")
@PreserveOnRefresh // do not call constructor when refresh page
@PermitAll
@Slf4j
@Deprecated
public class CleverestGamePageOld extends VerticalLayout implements HasUrlParameter<String>,
        BeforeEnterObserver,
        BeforeLeaveObserver,
        AfterNavigationObserver,
        Serializable {
    static final long serialVersionUID = 6789L;

    private String gameId;
    private boolean gameHost;
    private List<Registration> subs = new ArrayList<>();

    private CleverestGameSettingsComponent gameSettingsComponent = new CleverestGameSettingsComponent(new ArrayList<>());
    // NOTE: playBoardComponent is NOT a field anymore — a fresh instance is created
    // in configureAndAddPlayBoardComponent() each time. This avoids stale state and
    // double-attach issues caused by @PreserveOnRefresh reusing the same component tree.
    private CleverestWaitingRoomComponentOld waitingRoomComponent;
    private CleverestResultComponent resultComponent = new CleverestResultComponent();

    private QuestionService questionService;
    private GameService gameService;
    private CleverestBroadcaster broadcaster;
    private String originalLocation;

    // Each time on navigate from outside
    public CleverestGamePageOld(QuestionService questionService,
                                GameService gameService,
                                CleverestBroadcaster broadcaster) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.broadcaster = broadcaster;
        logState("Constructor", false);
    }

    @Override
    // Each time on refresh and navigate
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        this.gameId = parameter;
        logState("SetParameter", false);
    }

    private void renderSettings() {
        gameSettingsComponent.setQuestions(questionService.findAllCreatedByCurrentUser());
        final var settingsCompleteEvent = gameSettingsComponent.addSettingsCompletedListener(event -> {
            String newGameId = UUID.randomUUID().toString();
            gameService.createIfNotExists(newGameId, "Cleverest", GameType.CLEVEREST);
            broadcaster.createState(
                    newGameId,
                    getLoggedUser(),
                    event.getFirstRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                    event.getSecondRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                    event.getThirdRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList())
            );
            getUI().ifPresent(ui -> ui.navigate(this.getClass(), newGameId));
        });
        add(gameSettingsComponent);
        subs.add(settingsCompleteEvent);
    }

    /*
     * When page refresh happens, restore page state based on what's in DB + broadcaster.
     */
    private void renderComponents(GameEntity gameEntity, AfterNavigationEvent event, UI ui) {
        GameStatus status = gameEntity.getStatus();

        if (status.equals(GameStatus.NOT_STARTED)) {
            waitingRoomComponent = new CleverestWaitingRoomComponentOld(
                    gameId,
                    broadcaster,
                    gameHost,
                    getUI());
            add(waitingRoomComponent);
        } else if (status.equals(GameStatus.STARTED)) {
            configureAndAddPlayBoardComponent(event.isRefreshEvent(), ui);
        } else if (status.equals(GameStatus.FINISHED)) {
            CleverestGameState state = broadcaster.getState(gameId);
            configureAndAddResultComponent(state);
        }
    }

    /**
     * Creates a fresh PlayBoardComponent every time.
     * Passing UI explicitly avoids relying on getUI() inside the component
     * before it is attached to the layout.
     */
    private void configureAndAddPlayBoardComponent(boolean refreshEvent, UI ui) {
        if (notInGameOrCreator(gameId)) {
            navigateToNewGamePage("Игра уже началась, вы там не учавствуете", ui);
            return;
        }
        // Always create a new instance — never reuse a detached/stale component
        CleverestGamePlayBoardComponentOld playBoardComponent = new CleverestGamePlayBoardComponentOld();
        playBoardComponent.setState(gameId, broadcaster, gameHost, refreshEvent, ui);
        add(playBoardComponent);
    }

    private void configureAndAddResultComponent(CleverestGameState gameState) {
        resultComponent = new CleverestResultComponent();
        resultComponent.setState(
                gameState.getAllUserStates(),
                gameState.getHistory(),
                gameHost ? "" : getLoggedUser()
        );
        add(resultComponent);
    }

    @Override
    // Each time on refresh and navigate
    public void beforeEnter(BeforeEnterEvent event) {
        logState("BeforeEnter", false);
    }

    // Each time on refresh and navigate from outside
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState("OnAttach", false);
    }

    @Override
    // Each time on refresh and navigate
    public void afterNavigation(AfterNavigationEvent event) {
        logState("AfterNavigation", true);
        removeAll();
        clearSubs();
        originalLocation = event.getLocation().getPathWithQueryParameters();
        UI ui = getUI().orElseThrow(() -> new IllegalStateException("No UI"));

        if (StringUtils.isBlank(gameId)) {
            renderSettings();
            return;
        }
        GameEntity gameEntity = gameService.findById(gameId);
        if (gameEntity == null) {
            navigateToNewGamePage("Игра не существует", ui);
            return;
        }
        if (broadcaster.getState(gameId) == null) {
            navigateToNewGamePage("Состояние игры не создано", ui);
            return;
        }
        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
        subOnEvents(ui);
        renderComponents(gameEntity, event, ui);
        logState("AfterNavigation", false);
    }

    @Override
    // Each time on navigate outside
    public void beforeLeave(BeforeLeaveEvent event) {
        logState("BeforeLeave", true);
        if (StringUtils.isBlank(gameId) ||
                broadcaster.getState(gameId) == null
                || notInGameOrCreator(gameId)) {
            event.postpone().proceed();
            return;
        }
        if (gameService.findById(gameId).getStatus().equals(GameStatus.STARTED)) {
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Нельзя покинуть игру!");
            confirmDialog.setCloseOnOutsideClick(true);
            confirmDialog.addDialogCloseActionListener(e -> confirmDialog.close());
            confirmDialog.open();
            event.postpone();
            event.getUI().getPage().getHistory().replaceState(null, originalLocation);
        }
        logState("BeforeLeave", false);
    }

    // Each time on refresh and navigate outside
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        logState("OnDetach", true);
        clearSubs();
        logState("OnDetach", false);
    }

    private void subOnEvents(UI ui) {
        if (StringUtils.isEmpty(gameId)) {
            return;
        }
        subs.add(broadcaster.subscribe(
                gameId,
                AllUsersReadyEvent.class,
                event -> {
                    if (gameHost) {
                        gameService.updateStatus(gameId, GameStatus.STARTED);
                        gameService.linkQuestionsAndUsersWithGame(
                                gameId,
                                event.getUsernames(),
                                Stream.concat(
                                                broadcaster.getState(gameId).getFirstQuestions().stream(),
                                                broadcaster.getState(gameId).getSecondQuestions().stream())
                                        .toList());
                    }
                    // removeAll() causes onDetach on any existing PlayBoardComponent,
                    // which safely clears its own subs via its onDetach safety-net.
                    // Then configureAndAddPlayBoardComponent creates a fresh one.
                    QuizUtils.runActionInUi(Optional.ofNullable(ui), () -> {
                        removeAll();
                        configureAndAddPlayBoardComponent(false, ui);
                    });
                }));

        if (gameHost) {
            subs.add(broadcaster.subscribe(gameId,
                    SaveUsersAnswersEvent.class,
                    event -> gameService.submitAnswersBatch(
                            gameId,
                            event.getQuestion(),
                            event.getUserStateSnapshots())));
            subs.add(broadcaster.subscribe(
                    gameId,
                    GameFinishedEvent.class,
                    event -> gameService.finishGame(gameId)));
            subs.add(broadcaster.subscribe(
                    gameId,
                    QuestionGradedEvent.class,
                    event -> questionService.updateQuestionGrade(
                            event.getQuestion().getId(),
                            event.getUsername(),
                            event.getGrade())));
        }
    }

    private boolean notInGameOrCreator(String gameId) {
        CleverestGameState state = broadcaster.getState(gameId);
        return !state.userPresent(getLoggedUser())
                && !state.getGameHostName().equals(getLoggedUser());
    }

    private void navigateToNewGamePage(String notificationText, UI ui) {
        QuizComponents.infoNotification(notificationText);
        ui.navigateToClient("/");
    }

    private void clearSubs() {
        subs.forEach(Registration::remove);
        subs.clear();
    }

    private void logState(String action, boolean start) {
        log.info("[FIX][GamePage={}] {} [{}], User [{}], UI [{}], Subs size=[{}], items[{}]",
                this.hashCode(), start ? "Start" : "End", action, getLoggedUser(), getUI().map(Object::hashCode).orElse(-1), subs.size(), subs);
    }
}
