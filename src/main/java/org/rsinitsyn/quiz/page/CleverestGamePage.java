package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
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
import org.rsinitsyn.quiz.component.cleverest.CleverestWaitingRoomComponent;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "/cleverest", layout = MainLayout.class)
@PageTitle("Cleverest")
@PreserveOnRefresh
public class CleverestGamePage extends VerticalLayout implements HasUrlParameter<String>, BeforeLeaveObserver {

    private String gameId;
    private boolean isAdmin;
    private List<Registration> subscriptions = new ArrayList<>();

    private CleverestGameSettingsComponent gameSettingsComponent;
    private CleverestGamePlayBoardComponent playBoardComponent = new CleverestGamePlayBoardComponent();
    private CleverestWaitingRoomComponent waitingRoomComponent;

    private QuestionService questionService;
    private GameService gameService;
    private CleverestBroadcaster broadcaster;

    @Autowired
    public CleverestGamePage(QuestionService questionService,
                             GameService gameService,
                             CleverestBroadcaster broadcaster) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        removeAll();
        if (StringUtils.isBlank(parameter)) {
            renderSettings();
            return;
        }
        this.gameId = parameter;
        GameEntity gameEntity = gameService.findById(gameId);
        if (gameEntity == null) {
            Notification.show("Игра не существует");
            event.getUI().navigate(NewGamePage.class);
            return;
        }
        if (broadcaster.getState(gameId) == null) {
            Notification.show("Состояние игры не создано");
            event.getUI().navigate(NewGamePage.class);
            return;
        }

        this.isAdmin = gameEntity.getCreatedBy().equals(SessionWrapper.getLoggedUser());
        subscribeOnEvens(event.getUI());
        renderComponents(gameEntity, event.getUI());
    }

    private void renderSettings() {
        gameSettingsComponent = new CleverestGameSettingsComponent(questionService.findAllCreatedByCurrentUser());
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
                    getUI().ifPresent(ui -> ui.navigate(this.getClass(), newGameId));
                });
        add(gameSettingsComponent);

        subscriptions.addAll(List.of(
                settCompletedEvent
        ));
    }

    private void renderComponents(GameEntity gameEntity, UI ui) {
        GameStatus status = gameEntity.getStatus();

        if (status.equals(GameStatus.NOT_STARTED)) {
            waitingRoomComponent = new CleverestWaitingRoomComponent(
                    gameId,
                    broadcaster,
                    isAdmin);

            add(waitingRoomComponent);
        } else if (status.equals(GameStatus.STARTED)) {
            if (!isInGameOrCreator(gameId)) {
                ui.navigate(NewGamePage.class);
                Notification.show("Игра уже началась, вы там не учавствуете");
                return;
            }
            configurePlayBoardComponent();
            add(playBoardComponent);
        }
    }

    private boolean isInGameOrCreator(String gameId) {
        CleverestGameState state = broadcaster.getState(gameId);
        return state.getUsers().containsKey(SessionWrapper.getLoggedUser())
                || state.getCreatedBy().equals(SessionWrapper.getLoggedUser());
    }


    @Override
    protected void onAttach(AttachEvent attachEvent) {
        System.out.println(attachEvent);
    }

    private void subscribeOnEvens(UI ui) {
        subscriptions.add(broadcaster.subscribe(
                gameId,
                CleverestBroadcaster.AllUsersReadyEvent.class, event -> {
                    if (isAdmin) {
                        List<QuestionModel> firstAndSecondRoundQuestions =
                                Stream.concat(
                                        broadcaster.getState(gameId).getFirstQuestions().stream(),
                                        broadcaster.getState(gameId).getSecondQuestions().stream()
                                ).collect(Collectors.toList());
                        gameService.update(gameId, "Cleverest", GameStatus.STARTED);
                        gameService.linkQuestionsAndUsersWithGame(
                                gameId,
                                event.getUsernames(),
                                firstAndSecondRoundQuestions
                        );
                    }
                    QuizUtils.runActionInUi(Optional.ofNullable(ui), () -> {
                        removeAll();
                        configurePlayBoardComponent();
                        add(playBoardComponent);
                    });
                })
        );

        if (isAdmin) {
            subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GameFinishedEvent.class, event -> {
                gameService.finishGame(gameId);
            }));
        }
    }


    private void configurePlayBoardComponent() {
        playBoardComponent.setProps(gameId, broadcaster, isAdmin);
        subscriptions.add(playBoardComponent.subscribe(CleverestBroadcaster.SaveUserAnswersEvent.class, event -> {
            if (isAdmin) {
                gameService.submitAnswersBatch(gameId, event.getQuestion(), event.getUserStates());
            }
        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subscriptions.forEach(Registration::remove);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (StringUtils.isBlank(gameId) ||
                broadcaster.getState(gameId) == null
                || !isInGameOrCreator(gameId)) {
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
}
