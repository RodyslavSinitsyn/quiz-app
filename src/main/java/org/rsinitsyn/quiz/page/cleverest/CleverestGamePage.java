package org.rsinitsyn.quiz.page.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestGamePlayBoardComponent;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.page.MainPage;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.vaadin.flow.router.NavigationTrigger.*;
import static org.rsinitsyn.quiz.entity.GameStatus.STARTED;
import static org.rsinitsyn.quiz.utils.QuizComponents.infoNotification;
import static org.rsinitsyn.quiz.utils.QuizComponents.openConfirmDialog;
import static org.rsinitsyn.quiz.utils.QuizUtils.logState;
import static org.rsinitsyn.quiz.utils.QuizUtils.runActionInUi;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Route(value = "cleverest/game", layout = MainLayout.class)
@PageTitle("Cleverest - Игра")
@PermitAll
@Slf4j
@PreserveOnRefresh
public class CleverestGamePage extends VerticalLayout
        implements HasUrlParameter<String>, BeforeEnterObserver, BeforeLeaveObserver {

    private static final Set<NavigationTrigger> TRIGGERS = Set.of(
            REFRESH, PROGRAMMATIC, UI_NAVIGATE
    );

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;
    private final QuestionService questionService;
    private final PageValidator pageValidator;

    private String gameId;
    private boolean gameHost;
    private boolean isRefresh = false;
    private final List<Registration> subscriptions = new ArrayList<>();

    public CleverestGamePage(GameService gameService,
                             CleverestBroadcaster broadcaster,
                             QuestionService questionService,
                             final PageValidator pageValidator) {
        this.gameService = gameService;
        this.broadcaster = broadcaster;
        this.questionService = questionService;
        this.pageValidator = pageValidator;
    }

    @Override
    public void setParameter(BeforeEvent event, String gameId) {
        this.gameId = gameId;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        logState(this, event.getUI(), "beforeEnter", true, subscriptions);
        isRefresh = event.isRefreshEvent();
        final var result = pageValidator.validate(event, gameId, STARTED);
        if (result.navigationRequired()) {
            result.navigateAction().ifPresent(a -> a.accept(event));
            runActionInUi(event.getUI(), () -> result.notificationMessage().ifPresent(QuizComponents::infoNotification));
            return;
        }
        final var gameEntity = result.game();
        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
        final var state = broadcaster.getState(gameId);

        if (!gameHost && !state.userPresent(getLoggedUser())) {
            event.forwardTo(MainPage.class);
            runActionInUi(event.getUI(), () -> infoNotification("Игра уже началась, вы там не участвуете"));
            return;
        }
        logState(this, event.getUI(), "beforeEnter", false, subscriptions);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", true, subscriptions);
        if (gameId == null) {
            return;
        }
        final var ui = attachEvent.getUI();

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

            subscriptions.add(playBoard.addUpdateQuestionGradeEventListener(event ->
                    questionService.updateQuestionGrade(
                            event.question().getId(),
                            event.username(),
                            event.grade())));
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
        logState(this, event.getUI(), "beforeLeave", true, subscriptions);
        final var leaveAction = event.postpone();
        openConfirmDialog(
                new Span("Можно будет продолжить позже"),
                "Покинуть игру?",
                leaveAction::proceed
        );
        logState(this, event.getUI(), "beforeLeave", false, subscriptions);
    }
}
