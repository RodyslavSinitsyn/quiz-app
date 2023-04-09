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
import com.vaadin.flow.component.icon.Icon;
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
import org.rsinitsyn.quiz.service.CleverestBroadcastService;
import org.rsinitsyn.quiz.service.CleverestGameState;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class CleverestWaitingRoomComponent extends VerticalLayout {

    private String gameId;
    private boolean isAdmin;
    private Grid<CleverestGameState.UserGameState> usersGrid = new Grid<>(CleverestGameState.UserGameState.class, false);
    private Select<String> winnerBet = new Select<>();
    private Select<String> loserBet = new Select<>();

    private CleverestBroadcastService broadcastService;

    private List<Registration> subscriptions = new ArrayList<>();

    public CleverestWaitingRoomComponent(String gameId,
                                         CleverestBroadcastService broadcastService,
                                         boolean isAdmin) {
        this.gameId = gameId;
        this.isAdmin = isAdmin;
        this.broadcastService = broadcastService;
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
        dialog.setConfirmText("Играть");

        Button joinButton = new Button(
                !broadcastService.getState(gameId).getUsers().containsKey(QuizUtils.getLoggedUser())
                        ? "Присоедениться"
                        : "Поменять настройки");
        joinButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        joinButton.addClickListener(event -> {
            dialog.removeAll();
            dialog.add(userDialogContent(
                    broadcastService.getState(gameId).getUsers().get(QuizUtils.getLoggedUser()),
                    dialog));
            dialog.open();
        });
        add(joinButton);
    }

    private VerticalLayout userDialogContent(CleverestGameState.UserGameState userGameState, ConfirmDialog dialog) {
        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        dialogLayout.setDefaultHorizontalComponentAlignment(Alignment.START);
        dialogLayout.setSpacing(true);
        dialogLayout.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.LIGHT);

        TextField playerName = new TextField("Имя");
        playerName.setValue(QuizUtils.getLoggedUser());
        playerName.setReadOnly(true);
        playerName.addClassNames(LumoUtility.FontSize.LARGE);

        Span chooseColor = new Span("Выберите цвет");
        ColorPicker colorPicker = new ColorPicker();

        Optional.ofNullable(userGameState)
                .ifPresent(uState -> {
                    colorPicker.setValue(uState.getColor());
                    winnerBet.setValue(uState.getWinnerBet());
                    loserBet.setValue(uState.getLoserBet());
                });

        dialogLayout.add(playerName, chooseColor, colorPicker, winnerBet, loserBet);

        dialog.addConfirmListener(event -> {
            broadcastService.addUserToGame(gameId,
                    QuizUtils.getLoggedUser(),
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
                                LumoUtility.FontWeight.LIGHT)))
                .setHeader("Имя игрока");
        usersGrid.addColumn(new ComponentRenderer<>(userGameState -> {
                    Div color = new Div();
                    color.setWidth("2em");
                    color.setHeight("2em");
                    color.getStyle().set("background-color", userGameState.getColor());
                    return color;
                }))
                .setHeader("Цвет");
        usersGrid.addColumn(new ComponentRenderer<>(userGameState -> {
                    Icon icon = StringUtils.isEmpty(userGameState.getWinnerBet())
                            ? CleverestComponents.cancelIcon()
                            : CleverestComponents.doneIcon();

                    return new Span(icon, new Span(userGameState.getWinnerBet()));
                }))
                .setHeader("Поставил на победителя");
        usersGrid.addColumn(new ComponentRenderer<>(userGameState -> {
                    Icon icon = StringUtils.isEmpty(userGameState.getLoserBet())
                            ? CleverestComponents.cancelIcon()
                            : CleverestComponents.doneIcon();

                    return new Span(icon, new Span(userGameState.getLoserBet()));
                }))
                .setHeader("Поставил на проигравшего");
        usersGrid.addThemeVariants();
        usersGrid.setAllRowsVisible(true);
        usersGrid.addClassNames(LumoUtility.FontSize.XLARGE);
        updatePlayersGrid("");
    }

    private Select<String> betSelect(boolean winner) {
        Select<String> select = new Select<>();
        select.setLabel("Сделайте ставку на " + (winner ? "победителя" : "проигравшего"));
        select.setItems(broadcastService.getState(gameId).getUsers().keySet());
        select.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                broadcastService.addBet(gameId, QuizUtils.getLoggedUser(), event.getValue(), winner);
            }
        });
        return select;
    }


    private void updatePlayersGrid(String userWhoMadeAction) {
        if (broadcastService.getState(gameId).getUsers() != null
                && !broadcastService.getState(gameId).getUsers().isEmpty()) {
            usersGrid.setItems(broadcastService.getState(gameId).getUsers().values());
        }
    }

    private void configureAdminComponents(String gameId) {
        Anchor link = new Anchor("http://localhost:8080/cleverest/" + gameId + "?player", "Invite link");
        link.getElement().setAttribute("target", "_blank");
        add(link);

        Anchor prodLink = new Anchor("http://192.168.0.106:8080/cleverest/" + gameId + "?player", "Prod Invite link");
        prodLink.getElement().setAttribute("target", "_blank");
        add(prodLink);

        Button button = CleverestComponents.primaryButton("Начать игру", e -> broadcastService.allPlayersReady(gameId));
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
                        updatePlayersGrid(event.getUsername());
                        winnerBet.setItems(broadcastService.getState(gameId).getUsers().keySet());
                        loserBet.setItems(broadcastService.getState(gameId).getUsers().keySet());
                    });
                }));

        subscriptions.add(
                broadcastService.subscribe(CleverestBroadcastService.UserBetEvent.class, event -> {
                    QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                        updatePlayersGrid(event.getUsername());
                    });
                }));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        subscriptions.forEach(Registration::remove);
    }
}
