package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
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
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents;
import org.rsinitsyn.quiz.component.custom.ColorPicker;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.primaryButton;
import static org.rsinitsyn.quiz.model.cleverest.UserGameState.userGameState;
import static org.rsinitsyn.quiz.utils.QuizComponents.uploadComponent;
import static org.rsinitsyn.quiz.utils.QuizUtils.logState;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Slf4j
public class CleverestWaitingRoomComponent extends VerticalLayout {

    private final boolean gameHost;
    private final AtomicReference<InputStream> photoHolder = new AtomicReference<>();
    private final AtomicReference<UserGameState> userGameState = new AtomicReference<>();

    private Grid<UserGameState> usersGrid = new Grid<>(UserGameState.class, false);
    private Select<String> winnerBet = new Select<>();
    private Select<String> loserBet = new Select<>();
    private Button joinButton;
    private Button startGameButton;

    private final List<Registration> subscriptions = new ArrayList<>();

    /**
     * UI is passed explicitly so subscriptions can be registered immediately
     * in the constructor, without waiting for onAttach.
     * This mirrors the pattern used in CleverestGamePlayBoardComponent.setState().
     */
    public CleverestWaitingRoomComponent(boolean gameHost,
                                         List<UserGameState> users) {
        logState(this, getUI(), "Constructor", true, subscriptions);
        this.gameHost = gameHost;
//        this.winnerBet = betSelect(true);
//        this.loserBet = betSelect(false);
        configurePlayersList(users);
        users.stream().filter(u -> u.getUsername().equals(getLoggedUser())).findFirst().ifPresent(userGameState::set);
        add(usersGrid);
        if (gameHost) {
            configureHostComponents(users.size());
        } else {
            configurePlayerComponents();
        }
        addProgressBar();
        logState(this, getUI(), "Constructor", true, subscriptions);
    }

//    private void subscribeOnEvents(UI ui) {
////        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UserJoinedEvent.class, event ->
////                runActionInUi(ui, () -> {
////                    //        if (!gameHost && getLoggedUser().equals(username)) {
////            joinButton.setText(
////                    broadcaster.getState(gameId).userPresent(event.getUsername())
////                            ? "Поменять настройки"
////                            : "Играть");
////        }
////        winnerBet.setItems(broadcaster.getState(gameId).getAllUsernames());
////        loserBet.setItems(broadcaster.getState(gameId).getAllUsernames());
////        if (gameHost) {
////            startGameButton.setEnabled(broadcaster.getState(gameId).usersPresent());
////        }
////                })));
//

    /// /        subscriptions.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UserBetEvent.class, event ->
    /// /                runActionInUi(ui, () -> updatePlayersGrid(event.getUsername()))));
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

        joinButton = new Button(userGameState.get() != null ? "Поменять настройки" : "Играть");
        joinButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        joinButton.addClickListener(event -> {
            dialog.removeAll();
            dialog.add(userDialogContent(dialog));
            dialog.open();
        });
        add(joinButton);
    }

    private VerticalLayout userDialogContent(ConfirmDialog dialog) {
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

        Optional.ofNullable(userGameState.get())
                .ifPresent(state -> {
                    colorPicker.setValue(state.getColor());
                    winnerBet.setValue(state.winnerBet().getKey());
                    loserBet.setValue(state.loserBet().getKey());
                });

        final var upload = uploadComponent("Фото", (buffer, event) -> {
            photoHolder.set(buffer.getInputStream(event.getFileName()));
        }, null, 1);

        dialogLayout.add(playerName, chooseColor, colorPicker, upload, winnerBet, loserBet);

        dialog.addConfirmListener(event -> {
            if (userGameState.get() == null) {
                fireEvent(new UserSubmitDataEvent(getLoggedUser(), colorPicker.getValue()));
            } else {
                fireEvent(new UserUpdatedDataEvent(userGameState.get().getUsername(), colorPicker.getValue()));
            }
            this.userGameState.set(userGameState(getLoggedUser(), colorPicker.getValue()));
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

    private void configurePlayersList(List<UserGameState> users) {
        usersGrid.setItems(users);
//        usersGrid.addColumn(new ComponentRenderer<>(userGameState ->
//                        avatar(userGameState.getPhoto(), AvatarVariant.LUMO_XLARGE)))
//                .setHeader("Фото");
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
    }

    @Deprecated
    private Select<String> betSelect(boolean winner) {
        Select<String> select = new Select<>();
        select.setWidthFull();
        select.setLabel("Сделайте ставку на " + (winner ? "победителя" : "проигравшего"));
//        select.setItems(broadcaster.getState(gameId).getAllUsernames());
//        select.addValueChangeListener(event -> {
//            if (event.isFromClient()) {
//                broadcaster.sendBetEvent(gameId, getLoggedUser(), event.getValue(), winner);
//            }
//        });
        return select;
    }

    private void configureHostComponents(int usersCount) {
        startGameButton = primaryButton("Начать игру", e -> fireEvent(new StartGameEvent()));
        startGameButton.setEnabled(usersCount > 0);
        add(startGameButton);
    }

    public void updateTable(final String whoJoined,
                            final List<UserGameState> users) {
        usersGrid.setItems(users);
        if (gameHost) {
            startGameButton.setEnabled(!users.isEmpty());
        } else {
            joinButton.setText(whoJoined.equals(getLoggedUser()) ? "Поменять настройки" : "Играть");
        }
//        winnerBet.setItems(broadcaster.getState(gameId).getAllUsernames());
//        loserBet.setItems(broadcaster.getState(gameId).getAllUsernames());
//        if (gameHost) {
//            startGameButton.setEnabled(broadcaster.getState(gameId).usersPresent());
//        }
    }

    @Getter
    @Accessors(fluent = true)
    public class WaitingRoomEvent extends ComponentEvent<CleverestWaitingRoomComponent> {

        public WaitingRoomEvent() {
            super(CleverestWaitingRoomComponent.this, true);
        }
    }

    @Getter
    @RequiredArgsConstructor
    @Accessors(fluent = true)
    @EqualsAndHashCode(of = {"username", "color"}, callSuper = false)
    @ToString(of = {"username", "color"})
    public class UserSubmitDataEvent extends WaitingRoomEvent {
        private final String username;
        private final String color;
    }

    @Getter
    @RequiredArgsConstructor
    @Accessors(fluent = true)
    @EqualsAndHashCode(of = "color", callSuper = false)
    @ToString(of = "color")
    public class UserUpdatedDataEvent extends WaitingRoomEvent {
        private final String username;
        private final String color;
    }

    public class StartGameEvent extends WaitingRoomEvent {
    }

    public Registration addUserSubmitDataEventListener(ComponentEventListener<UserSubmitDataEvent> listener) {
        return addListener(UserSubmitDataEvent.class, listener);
    }

    public Registration addUserUpdateDataEventListener(ComponentEventListener<UserUpdatedDataEvent> listener) {
        return addListener(UserUpdatedDataEvent.class, listener);
    }

    public Registration addStartGameEventListener(ComponentEventListener<StartGameEvent> listener) {
        return addListener(StartGameEvent.class, listener);
    }
}
