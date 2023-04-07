package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import org.rsinitsyn.quiz.service.CleverestBroadcastService;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class CleverestWaitingRoomComponent extends VerticalLayout {

    private String gameId;
    private Grid<String> usersGrid = new Grid<>(String.class, false);

    private CleverestBroadcastService broadcastService;

    private List<Registration> subscriptions = new ArrayList<>();

    public CleverestWaitingRoomComponent(String gameId,
                                         CleverestBroadcastService broadcastService,
                                         boolean isAdmin) {
        this.gameId = gameId;
        this.broadcastService = broadcastService;
        configurePlayersList();
        add(usersGrid);
        if (isAdmin) {
            configureAdminComponents(gameId);
        }
        addProgressBar();
    }


    private void addProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        Div progressBarLabel = new Div();
        progressBarLabel.setText("Игра еще не начата...");
        add(progressBarLabel, progressBar);
    }

    private void configurePlayersList() {
        usersGrid.addColumn(playerName -> playerName).setHeader("Имя игрока");
        updatePlayersGrid();
    }

    private void updatePlayersGrid() {
        if (broadcastService.getState(gameId).getUsers() != null
                && !broadcastService.getState(gameId).getUsers().isEmpty()) {
            usersGrid.setItems(broadcastService.getState(gameId).getUsers().keySet());
        }
    }

    private void configureAdminComponents(String gameId) {
        Anchor link = new Anchor("http://localhost:8080/cleverest/" + gameId + "?player", "Invite link");
        link.getElement().setAttribute("target", "_blank");
        add(link);

        Anchor prodLink = new Anchor("http://192.168.0.106:8080/cleverest/" + gameId + "?player", "Prod Invite link");
        prodLink.getElement().setAttribute("target", "_blank");
        add(prodLink);

        Button button = new Button("Начать игру");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClickListener(event -> {
            broadcastService.allPlayersReady(gameId);
        });
        add(button);
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        subscriptions.add(
                broadcastService.subscribe(CleverestBroadcastService.UserJoinedEvent.class, event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                        updatePlayersGrid();
                    });
                }));

        broadcastService.addUserToGame(gameId, QuizUtils.getLoggedUser());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subscriptions.forEach(Registration::remove);
    }
}
