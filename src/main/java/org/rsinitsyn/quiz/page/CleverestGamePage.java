package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestGamePlayBoardComponent;
import org.rsinitsyn.quiz.component.cleverest.CleverestGameSettingsComponent;
import org.rsinitsyn.quiz.component.cleverest.CleverestResultComponent;
import org.rsinitsyn.quiz.component.cleverest.CleverestWaitingRoomComponent;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.beans.factory.annotation.Autowired;

/*
    constructor
    setParameter
    beforeEnter
    onAttach
    afterNavigation
    beforeLeave
    onDetach
 */
@Route(value = "/cleverest", layout = MainLayout.class)
@PageTitle("Cleverest")
@PreserveOnRefresh // do not call constructor when refresh page
public class CleverestGamePage extends VerticalLayout implements HasUrlParameter<String>,
        BeforeEnterObserver,
        BeforeLeaveObserver,
        AfterNavigationObserver,
        Serializable {
    static final long serialVersionUID = 6789L;

    private String gameId;
    private boolean isAdmin;
    private List<Registration> subs = new ArrayList<>();

    private CleverestGameSettingsComponent gameSettingsComponent = new CleverestGameSettingsComponent(new ArrayList<>());
    private CleverestGamePlayBoardComponent playBoardComponent = new CleverestGamePlayBoardComponent();
    private CleverestWaitingRoomComponent waitingRoomComponent;
    private CleverestResultComponent resultComponent = new CleverestResultComponent();

    private QuestionService questionService;
    private GameService gameService;
    private CleverestBroadcaster broadcaster;

    @Autowired
    // Each time on navigate from outside
    public CleverestGamePage(QuestionService questionService,
                             GameService gameService,
                             CleverestBroadcaster broadcaster) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.broadcaster = broadcaster;
    }

    @Override
    // Each time on refresh and navigate
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        this.gameId = parameter;
    }

    private void renderSettings() {
        gameSettingsComponent.setQuestions(questionService.findAllCreatedByCurrentUser());
        Registration settCompletedEvent = gameSettingsComponent.addListener(CleverestGameSettingsComponent.SettingsCompletedEvent.class,
                event -> {
                    String newGameId = UUID.randomUUID().toString();
                    gameService.createIfNotExists(newGameId, GameType.CLEVEREST);
                    broadcaster.createState(
                            newGameId,
                            SessionWrapper.getLoggedUser(),
                            event.getFirstRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                            event.getSecondRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                            event.getThirdRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList())
                    );
                    getUI().ifPresent(ui -> {
                        ui.navigate(this.getClass(), newGameId);
                    });
                });
        add(gameSettingsComponent);
        subs.addAll(List.of(settCompletedEvent));
    }

    /*
        When page refresh happens restore page state
     */
    private void renderComponents(GameEntity gameEntity,
                                  AfterNavigationEvent event) {
        GameStatus status = gameEntity.getStatus();

        if (status.equals(GameStatus.NOT_STARTED)) {
            waitingRoomComponent = new CleverestWaitingRoomComponent(
                    gameId,
                    broadcaster,
                    isAdmin);
            add(waitingRoomComponent);
        } else if (status.equals(GameStatus.STARTED)) {
            configureAndAddPlayBoardComponent(event.isRefreshEvent());
        } else if (status.equals(GameStatus.FINISHED)) {
            CleverestGameState state = broadcaster.getState(gameId);
            configureAndAddResultComponent(state);
        }
    }

    private void configureAndAddPlayBoardComponent(boolean refreshEvent) {
        if (notInGameOrCreator(gameId)) {
            navigateToNewGamePage("Игра уже началась, вы там не учавствуете", getUI().orElseThrow());
            return;
        }
        playBoardComponent.setState(gameId, broadcaster, isAdmin, refreshEvent);
        add(playBoardComponent);
    }

    private void configureAndAddResultComponent(CleverestGameState gameState) {
        resultComponent = new CleverestResultComponent();
        resultComponent.setState(
                gameState.getUsers().values(),
                gameState.getHistory(),
                isAdmin ? "" : SessionWrapper.getLoggedUser()
        );
        add(resultComponent);
    }


    @Override
    // Each time on refresh and navigate
    public void beforeEnter(BeforeEnterEvent event) {
    }

    // Each time on refresh and navigate from outside
    @Override
    protected void onAttach(AttachEvent attachEvent) {
    }

    @Override
    // Each time on refresh and navigate
    public void afterNavigation(AfterNavigationEvent event) {
        removeAll();
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
        this.isAdmin = gameEntity.getCreatedBy().equals(SessionWrapper.getLoggedUser());

        subOnEvents(ui);
        renderComponents(gameEntity, event);
    }

    @Override
    // Each time on navigate outside
    public void beforeLeave(BeforeLeaveEvent event) {
        if (StringUtils.isBlank(gameId) ||
                broadcaster.getState(gameId) == null
                || notInGameOrCreator(gameId)) {
            event.postpone().proceed();
            return;
        }
        if (gameService.findById(gameId).getStatus().equals(GameStatus.STARTED)) {
            event.postpone();
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Нельзя покинуть игру!");
            confirmDialog.setCloseOnOutsideClick(true);
            confirmDialog.addDialogCloseActionListener(e -> {
                confirmDialog.close();
            });
            confirmDialog.open();
        }
    }

    // Each time on refresh and navigate outside
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        clearSubs();
    }


    private void subOnEvents(UI ui) {
        if (StringUtils.isEmpty(gameId)) {
            return;
        }
        subs.add(broadcaster.subscribe(
                gameId,
                CleverestBroadcaster.AllUsersReadyEvent.class, event -> {
                    if (isAdmin) {
                        gameService.update(gameId, "Cleverest", GameStatus.STARTED);
                        gameService.linkQuestionsAndUsersWithGame(
                                gameId,
                                event.getUsernames(),
                                Stream.concat(
                                                broadcaster.getState(gameId).getFirstQuestions().stream(),
                                                broadcaster.getState(gameId).getSecondQuestions().stream())
                                        .toList());
                    }
                    QuizUtils.runActionInUi(Optional.ofNullable(ui), () -> {
                        removeAll();
                        configureAndAddPlayBoardComponent(false);
                    });
                })
        );
        if (isAdmin) {
            subs.add(broadcaster.subscribe(gameId,
                    CleverestBroadcaster.SaveUserAnswersEvent.class,
                    event -> gameService.submitAnswersBatch(gameId, event.getQuestion(), event.getUserStates())));
            subs.add(broadcaster.subscribe(
                    gameId,
                    CleverestBroadcaster.GameFinishedEvent.class,
                    event -> gameService.finishGame(gameId)
            ));
            subs.add(broadcaster.subscribe(
                    gameId,
                    CleverestBroadcaster.QuestionGradedEvent.class,
                    event -> {
                        questionService.updateQuestionGrade(
                                event.getQuestion().getId(),
                                event.getUsername(),
                                event.getGrade());
                    }
            ));
        }
    }

    private boolean notInGameOrCreator(String gameId) {
        CleverestGameState state = broadcaster.getState(gameId);
        return !state.getUsers().containsKey(SessionWrapper.getLoggedUser())
                && !state.getCreatedBy().equals(SessionWrapper.getLoggedUser());
    }

    private void navigateToNewGamePage(String notificationText, UI ui) {
        QuizComponents.infoNotification(notificationText);
        ui.navigateToClient("/");
    }

    private void clearSubs() {
        subs.forEach(Registration::remove);
        subs.clear();
    }
}
