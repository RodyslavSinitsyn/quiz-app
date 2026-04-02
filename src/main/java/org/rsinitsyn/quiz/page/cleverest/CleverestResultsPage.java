package org.rsinitsyn.quiz.page.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestResultComponent;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.QuizComponents;

import static org.rsinitsyn.quiz.entity.GameStatus.FINISHED;
import static org.rsinitsyn.quiz.utils.QuizUtils.runActionInUi;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Route(value = "cleverest/results", layout = MainLayout.class)
@PageTitle("Cleverest - Результаты")
@PermitAll
@Slf4j
public class CleverestResultsPage extends VerticalLayout
        implements HasUrlParameter<String>, BeforeEnterObserver {

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;
    private final PageValidator pageValidator;

    private String gameId;
    private boolean gameHost;

    public CleverestResultsPage(GameService gameService,
                                CleverestBroadcaster broadcaster,
                                final PageValidator pageValidator) {
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
        final var result = pageValidator.validate(gameId, FINISHED);
        if (result.navigationRequired()) {
            result.navigateAction().ifPresent(a -> a.accept(event));
            runActionInUi(event.getUI(), () -> result.notificationMessage().ifPresent(QuizComponents::infoNotification));
            return;
        }
        final var gameEntity = result.game();
        this.gameHost = gameEntity.getCreatedBy().equals(getLoggedUser());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (gameId == null) return;

        final var state = broadcaster.getState(gameId);
        final var resultComponent = new CleverestResultComponent();
        resultComponent.setState(
                state.getAllUserStates(),
                state.getHistory(),
                gameHost ? "" : getLoggedUser()
        );
        final var newGameButton = new Button("Новая игра",
                e -> attachEvent.getUI().navigate(CleverestSetupPage.class));
        newGameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        add(resultComponent, newGameButton);
    }
}
