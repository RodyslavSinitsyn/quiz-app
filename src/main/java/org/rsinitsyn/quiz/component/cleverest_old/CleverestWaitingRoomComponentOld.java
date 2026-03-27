package org.rsinitsyn.quiz.component.cleverest_old;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.custom.ColorPicker;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.primaryButton;
import static org.rsinitsyn.quiz.utils.QuizComponents.uploadComponent;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Deprecated
@Slf4j
public class CleverestWaitingRoomComponentOld extends VerticalLayout {

    private String gameId;
    private boolean gameHost;
    private Grid<UserGameState> usersGrid = new Grid<>(UserGameState.class, false);
    private Select<String> winnerBet = new Select<>();
    private Select<String> loserBet = new Select<>();

    private Button joinButton;
    private Button startGameButton;

    private final CleverestBroadcaster broadcaster;
    private final List<Registration> subscriptions = new ArrayList<>();
    private final AtomicReference<InputStream> photoHolder = new AtomicReference<>();

    /**
     * UI is passed explicitly so subscriptions can be registered immediately
     * in the constructor, without waiting for onAttach.
     * This mirrors the pattern used in CleverestGamePlayBoardComponent.setState().
     */
    public CleverestWaitingRoomComponentOld(String gameId,
                                            CleverestBroadcaster broadcaster,
                                            boolean gameHost,
                                            Optional<UI> ui) {
        this.gameId = gameId;
        this.gameHost = gameHost;
        this.broadcaster = broadcaster;
        this.winnerBet = betSelect(true);
        this.loserBet = betSelect(false);
        configurePlayersList();
        add(usersGrid);
        if (gameHost) {
            configureHostComponents(gameId);
        } else {
            configurePlayerComponents();
        }
        addProgressBar();

        // Register subscriptions here with the known-good UI reference,
        // not in onAttach where the UI reference may shift during a refresh cycle.
//        ui.ifPresent(this::subscribeOnEvents);
    }

//    private void subscribeOnEvents(UI ui) {
//        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UserJoinedEvent.class, event ->
//                runActionInUi(ui, () -> {
//                    updatePlayersGrid(event.getUser().getUsername());
//                    if (!gameHost && getLoggedUser().equals(event.getUser().getUsername())) {
//                        joinButton.setText(
//                                broadcaster.getState(gameId).userPresent(event.getUser().getUsername())
//                                        ? "Поменять настройки"
//                                        : "Играть");
//                    }
//                    winnerBet.setItems(broadcaster.getState(gameId).getAllUsernames());
//                    loserBet.setItems(broadcaster.getState(gameId).getAllUsernames());
//                    if (gameHost) {
//                        startGameButton.setEnabled(broadcaster.getState(gameId).usersPresent());
//                    }
//                })));
//
//        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UserBetEvent.class, event ->
//                runActionInUi(ui, () -> updatePlayersGrid(event.getUser().getUsername()))));
//
//        log.trace("subscribeOnEvents. subscriptions count: {}", subscriptions.size());
//    }

    // onAttach is intentionally empty — subscriptions are in subscribeOnEvents()
    @Override
    protected void onAttach(AttachEvent attachEvent) {
//        subscribeOnEvents(attachEvent.getUI());
    }

    // onDetach cleans up subscriptions as a safety net
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.trace("onDetach. unsubscribe {}", subscriptions.size());
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }

    private void configurePlayerComponents() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setCloseOnEsc(true);
        dialog.setRejectable(false);
        dialog.setCancelable(true);
        dialog.setCancelText("Назад");
        dialog.setConfirmText("Сохранить");

        joinButton = new Button("Играть");
        joinButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        joinButton.addClickListener(event -> {
            dialog.removeAll();
            dialog.add(userDialogContent(
                    broadcaster.getState(gameId).getUserState(getLoggedUser()),
                    dialog));
            dialog.open();
        });
        add(joinButton);
    }

    private VerticalLayout userDialogContent(UserGameState userGameState, ConfirmDialog dialog) {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        dialogLayout.setDefaultHorizontalComponentAlignment(Alignment.START);
        dialogLayout.setSpacing(true);
        dialogLayout.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.LIGHT);

        TextField playerName = new TextField("Имя");
        playerName.setValue(getLoggedUser());
        playerName.setReadOnly(true);
        playerName.addClassNames(LumoUtility.FontSize.LARGE);

        Span chooseColor = new Span("Выберите цвет");
        ColorPicker colorPicker = new ColorPicker();

        Optional.ofNullable(userGameState)
                .ifPresent(uState -> {
                    colorPicker.setValue(uState.getColor());
                    winnerBet.setValue(uState.winnerBet().getKey());
                    loserBet.setValue(uState.loserBet().getKey());
                });

        final var upload = uploadComponent("Фото", (buffer, event) -> {
            photoHolder.set(buffer.getInputStream(event.getFileName()));
        }, null, 1);

        dialogLayout.add(playerName, chooseColor, colorPicker, upload, winnerBet, loserBet);

        dialog.addConfirmListener(event -> {
            broadcaster.sendJoinUserEvent(gameId,
                    getLoggedUser(),
                    defaultIfEmpty(colorPicker.getValue(), "#000000"),
                    photoHolder.get(),
                    winnerBet.getValue(),
                    loserBet.getValue());
        });

        return dialogLayout;
    }

    private void addProgressBar() {
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        Div progressBarLabel = new Div();
        progressBarLabel.setText("Игра еще не начата...");
        add(progressBarLabel, progressBar);
    }

    private void configurePlayersList() {
//        usersGrid.addColumn(new ComponentRenderer<>(userGameState ->
//                userProfile(userGameState.snapshot()))).setHeader("Имя игрока");
        usersGrid.addColumn(new ComponentRenderer<>(userGameState -> {
            Div color = new Div();
            color.setWidth("2em");
            color.setHeight("2em");
            color.getStyle().set("background-color", userGameState.getColor());
            return color;
        })).setHeader("Цвет");
        usersGrid.addColumn(new ComponentRenderer<>(userGameState -> new Span(
                userGameState.winnerBet().getKey().isEmpty()
                        ? CleverestComponents.cancelIcon()
                        : CleverestComponents.doneIcon(),
                userGameState.loserBet().getKey().isEmpty()
                        ? CleverestComponents.cancelIcon()
                        : CleverestComponents.doneIcon()
        ))).setHeader("Ставки");
        usersGrid.addThemeVariants();
        usersGrid.setAllRowsVisible(true);
        usersGrid.addClassNames(LumoUtility.FontSize.XLARGE);
        updatePlayersGrid("");
    }

    private Select<String> betSelect(boolean winner) {
        Select<String> select = new Select<>();
        select.setWidthFull();
        select.setLabel("Сделайте ставку на " + (winner ? "победителя" : "проигравшего"));
        select.setItems(broadcaster.getState(gameId).getAllUsernames());
        select.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                broadcaster.sendUserBetEvent(gameId, getLoggedUser(), event.getValue(), winner);
            }
        });
        return select;
    }

    private void updatePlayersGrid(String userWhoMadeAction) {
        if (broadcaster.getState(gameId).usersPresent()) {
            usersGrid.setItems(broadcaster.getState(gameId).getAllUserStates());
        }
    }

    private void configureHostComponents(String gameId) {
        Anchor link = new Anchor("http://localhost:8080/cleverest/" + gameId + "?player", "Invite link");
        link.getElement().setAttribute("target", "_blank");
        add(link);

        // TODO PUBLIC HOST
        Anchor prodLink = new Anchor("http://192.168.0.107:8080/cleverest/" + gameId + "?player", "Prod Invite link");
        prodLink.getElement().setAttribute("target", "_blank");
        add(prodLink);

        startGameButton = primaryButton("Начать игру", e -> broadcaster.sendUsersReadyEvent(gameId));
        startGameButton.setEnabled(!broadcaster.getState(gameId).usersPresent());
        add(startGameButton);
    }
}

