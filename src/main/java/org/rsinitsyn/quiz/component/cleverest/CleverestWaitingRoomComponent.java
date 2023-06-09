package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.сustom.ColorPicker;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;

public class CleverestWaitingRoomComponent extends VerticalLayout {

    private String gameId;
    private boolean isAdmin;
    private Grid<UserGameState> usersGrid = new Grid<>(UserGameState.class, false);
    private Select<String> winnerBet = new Select<>();
    private Select<String> loserBet = new Select<>();

    private Button joinButton;
    private Button startGameButton;

    private CleverestBroadcaster broadcaster;

    private List<Registration> subscriptions = new ArrayList<>();

    public CleverestWaitingRoomComponent(String gameId,
                                         CleverestBroadcaster broadcaster,
                                         boolean isAdmin) {
        this.gameId = gameId;
        this.isAdmin = isAdmin;
        this.broadcaster = broadcaster;
        this.winnerBet = betSelect(true);
        this.loserBet = betSelect(false);
        configurePlayersList();
        add(usersGrid);
        if (isAdmin) {
            configureAdminComponents(gameId);
        } else {
            configurePlayerComponents();
        }
        addProgressBar();
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
                    broadcaster.getState(gameId).getUsers().get(SessionWrapper.getLoggedUser()),
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
        playerName.setValue(SessionWrapper.getLoggedUser());
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

        dialogLayout.add(playerName, chooseColor, colorPicker, winnerBet, loserBet);

        dialog.addConfirmListener(event -> {
            broadcaster.sendJoinUserEvent(gameId,
                    SessionWrapper.getLoggedUser(),
                    StringUtils.defaultIfEmpty(colorPicker.getValue(), "#000000"),
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
        usersGrid.addColumn(new ComponentRenderer<>(userGameState ->
                CleverestComponents.userNameSpan(
                        userGameState.getUsername(),
                        userGameState.getColor(),
                        LumoUtility.FontWeight.LIGHT))).setHeader("Имя игрока");
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
        select.setItems(broadcaster.getState(gameId).getUsers().keySet());
        select.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                broadcaster.sendBetEvent(gameId, SessionWrapper.getLoggedUser(), event.getValue(), winner);
            }
        });
        return select;
    }


    private void updatePlayersGrid(String userWhoMadeAction) {
        if (broadcaster.getState(gameId).getUsers() != null
                && !broadcaster.getState(gameId).getUsers().isEmpty()) {
            usersGrid.setItems(broadcaster.getState(gameId).getUsers().values());
        }
    }

    private void configureAdminComponents(String gameId) {
        Anchor link = new Anchor("http://localhost:8080/cleverest/" + gameId + "?player", "Invite link");
        link.getElement().setAttribute("target", "_blank");
        add(link);

        // TODO PUBLIC HOST
        Anchor prodLink = new Anchor("http://192.168.0.107:8080/cleverest/" + gameId + "?player", "Prod Invite link");
        prodLink.getElement().setAttribute("target", "_blank");
        add(prodLink);

        startGameButton = CleverestComponents.primaryButton("Начать игру", e -> broadcaster.sendPlayersReadyEvent(gameId));
        startGameButton.setEnabled(!broadcaster.getState(gameId).getUsers().isEmpty());
        add(startGameButton);
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        subscriptions.add(
                broadcaster.subscribe(gameId, CleverestBroadcaster.UserJoinedEvent.class, event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                        updatePlayersGrid(event.getUsername());
                        if (SessionWrapper.getLoggedUser().equals(event.getUsername())) {
                            joinButton.setText(
                                    !broadcaster.getState(gameId).getUsers().containsKey(event.getUsername())
                                            ? "Играть"
                                            : "Поменять настройки");
                        }
                        winnerBet.setItems(broadcaster.getState(gameId).getUsers().keySet());
                        loserBet.setItems(broadcaster.getState(gameId).getUsers().keySet());
                        if (isAdmin) {
                            startGameButton.setEnabled(!broadcaster.getState(gameId).getUsers().isEmpty());
                        }
                    });
                }));

        subscriptions.add(
                broadcaster.subscribe(gameId,
                        CleverestBroadcaster.UserBetEvent.class,
                        event -> {
                            QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                                updatePlayersGrid(event.getUsername());
                            });
                        }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }
}
