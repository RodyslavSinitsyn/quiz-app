package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
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
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.CleverestBroadcastService;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizUtils;
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
    private CleverestBroadcastService broadcastService;

    @Autowired
    public CleverestGamePage(QuestionService questionService,
                             GameService gameService,
                             CleverestBroadcastService broadcastService) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.broadcastService = broadcastService;
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
            Notification.show("Game not exists");
            event.getUI().navigate(NewGamePage.class);
            return;
        }
        if (broadcastService.getState(gameId) == null) {
            Notification.show("Game state not exists");
            event.getUI().navigate(NewGamePage.class);
            return;
        }

        subscribeOnEvens(event.getUI());
        this.isAdmin = gameEntity.getCreatedBy().equals(QuizUtils.getLoggedUser());
        renderComponents(gameEntity);
    }

    private void renderSettings() {
        gameSettingsComponent = new CleverestGameSettingsComponent(questionService.findAllByCurrentUser());
        Registration settCompletedEvent = gameSettingsComponent.addListener(CleverestGameSettingsComponent.SettingsCompletedEvent.class,
                event -> {
                    String newGameId = UUID.randomUUID().toString();
                    gameService.createIfNotExists(newGameId, GameType.CLEVEREST);
                    broadcastService.createState(
                            newGameId,
                            QuizUtils.getLoggedUser(),
                            event.getFirstRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                            event.getSecondRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                            event.getThirdRound().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList()),
                            event.getSpecial().stream().map(e -> questionService.toQuizQuestionModel(e)).collect(Collectors.toList())
                    );
                    getUI().ifPresent(ui -> ui.navigate(this.getClass(), newGameId));
                });
        add(gameSettingsComponent);

        subscriptions.addAll(List.of(
                settCompletedEvent
        ));
    }

    private void renderComponents(GameEntity gameEntity) {
        GameStatus status = gameEntity.getStatus();

        if (status.equals(GameStatus.NOT_STARTED)) {
//            broadcastService.createState(gameId); // TODO Fix when reload page and entity exists in DB
            waitingRoomComponent = new CleverestWaitingRoomComponent(
                    gameId,
                    broadcastService,
                    isAdmin);

            add(waitingRoomComponent);
        } else if (status.equals(GameStatus.STARTED)) {
            configurePlayBoardComponent();
            add(playBoardComponent);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        System.out.println(attachEvent);
    }

    private void subscribeOnEvens(UI ui) {
        subscriptions.add(broadcastService.subscribe(
                CleverestBroadcastService.AllUsersReadyEvent.class, event -> {
                    if (isAdmin) {
                        List<QuizQuestionModel> firstAndSecondRoundQuestions =
                                Stream.concat(
                                        broadcastService.getState(gameId).getFirstQuestions().stream(),
                                        broadcastService.getState(gameId).getSecondQuestions().stream()
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
            subscriptions.add(broadcastService.subscribe(CleverestBroadcastService.GameFinishedEvent.class, event -> {
                gameService.finishGame(gameId);
            }));
        }
    }


    private void configurePlayBoardComponent() {
        playBoardComponent.setProperties(gameId, broadcastService, isAdmin);
        subscriptions.add(playBoardComponent.subscribe(CleverestBroadcastService.SaveUserAnswersEvent.class, event -> {
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
//        if (StringUtils.isBlank(gameId) || broadcastService.getState(gameId) == null) {
//            event.postpone().proceed();
//            return;
//        }
//        if (gameService.findById(gameId).getStatus().equals(GameStatus.STARTED)) {
//            BeforeLeaveEvent.ContinueNavigationAction leaveAction =
//                    event.postpone();
//            Dialog confirmDialog = new Dialog();
//            confirmDialog.setHeaderTitle("К сожалению нельзя покинуть игру!");
//            confirmDialog.setCloseOnOutsideClick(true);
//            confirmDialog.addDialogCloseActionListener(e -> {
//                confirmDialog.close();
//            });
//            confirmDialog.open();
//        }
    }
}
