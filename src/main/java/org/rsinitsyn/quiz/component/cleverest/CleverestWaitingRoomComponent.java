package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import org.rsinitsyn.quiz.component.theme.ThemePreset;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.utils.ThemeUtils;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.ofNullable;
import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.horizontalLayout;
import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.primaryButton;
import static org.rsinitsyn.quiz.utils.QuizComponents.uploadComponent;
import static org.rsinitsyn.quiz.utils.QuizUtils.logState;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;
import static org.rsinitsyn.quiz.utils.ThemeUtils.BLACK_COLOR;
import static org.rsinitsyn.quiz.utils.ThemeUtils.THEME_PRESETS;

@Slf4j
public class CleverestWaitingRoomComponent extends VerticalLayout {

    private final boolean hostPage;
    private final AtomicReference<InputStream> photoHolder = new AtomicReference<>();
    private final AtomicReference<UserProfile> userGameState = new AtomicReference<>();

    private final Grid<UserProfile> usersGrid = new Grid<>(UserProfile.class, false);
    private Select<String> winnerBet = new Select<>();
    private Select<String> loserBet = new Select<>();
    private Button joinButton;
    private Button startGameButton;

    public CleverestWaitingRoomComponent(boolean hostPage,
                                         List<UserProfile> users) {
        logState(this, getUI(), "Constructor", true, List.of());
        this.hostPage = hostPage;
        this.winnerBet = createBetComponent(true, users.stream().map(UserProfile::username).toList());
        this.loserBet = createBetComponent(false, users.stream().map(UserProfile::username).toList());
        configurePlayersList(users);
        users.stream().filter(u -> u.username().equals(getLoggedUser())).findFirst().ifPresent(userGameState::set);
        add(usersGrid);
        if (hostPage) {
            configureHostComponents(users.size());
        } else {
            configurePlayerComponents();
        }
        addProgressBar();
        logState(this, getUI(), "Constructor", true, List.of());
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
        final var themes = new ComboBox<ThemePreset>("Theme");
        themes.setItems(THEME_PRESETS);
        themes.setItemLabelGenerator(ThemePreset::name);
        themes.setRenderer(new ComponentRenderer<>(CleverestComponents::themeColor));

        themes.addValueChangeListener(event -> {
            final var preset = event.getValue();
            ThemeUtils.applyTheme(UI.getCurrent(), preset.color());
        });

//        ofNullable(userGameState.get())
//                .ifPresent(state -> {
//                    winnerBet.setValue(state.winnerBet().getKey());
//                    loserBet.setValue(state.loserBet().getKey());
//                });

        final var upload = uploadComponent("Фото", (buffer, event) -> {
            photoHolder.set(buffer.getInputStream(event.getFileName()));
        }, null, 1);

        dialogLayout.add(playerName, chooseColor, themes, upload);
//        TODO: Bets disabled for now
//        dialog.add(winnerBet, loserBet);

        dialog.addConfirmListener(event -> {
            fireEvent(new UserSubmitDataEvent(getLoggedUser(),
                    ofNullable(themes.getValue()).map(ThemePreset::color).orElse(BLACK_COLOR),
                    photoHolder.get(),
                    winnerBet.getValue(),
                    loserBet.getValue()));
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

    private void configurePlayersList(List<UserProfile> users) {
        usersGrid.setItems(users);
        usersGrid.addColumn(new ComponentRenderer<>(CleverestComponents::userProfile))
                .setHeader("Имя игрока");
//        usersGrid.addColumn(new ComponentRenderer<>(userGameState -> new Span(
//                userGameState.winnerBet().getKey().isEmpty()
//                        ? cancelIcon()
//                        : doneIcon(),
//                userGameState.loserBet().getKey().isEmpty()
//                        ? cancelIcon()
//                        : doneIcon()
//        ))).setHeader("Ставки");
        usersGrid.addThemeVariants();
        usersGrid.setAllRowsVisible(true);
        usersGrid.addClassNames(LumoUtility.FontSize.XLARGE);
    }

    @Deprecated
    private Select<String> createBetComponent(boolean winner,
                                              List<String> usernames) {
        final var select = new Select<String>();
        select.setWidthFull();
        select.setLabel("Сделайте ставку на " + (winner ? "победителя" : "проигравшего"));
        select.setItems(usernames);
        select.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                fireEvent(new UserBetEvent(getLoggedUser(), event.getValue(), winner));
            }
        });
        return select;
    }

    private void configureHostComponents(int usersCount) {
        startGameButton = primaryButton("Начать игру", e -> fireEvent(new StartGameEvent()));
        startGameButton.setEnabled(usersCount > 0);
        add(startGameButton);
    }

    public void updateUserState(UserProfile userProfile) {
        this.userGameState.set(userProfile);
        joinButton.setText(userProfile.username().equals(getLoggedUser())
                ? "Поменять настройки"
                : "Играть");
    }

    public void updateTableAndBets(final List<UserProfile> users) {
        usersGrid.setItems(users);
        usersGrid.getDataProvider().refreshAll();
        if (hostPage) {
            startGameButton.setEnabled(!users.isEmpty());
        }
        winnerBet.setItems(users.stream().map(UserProfile::username).toList());
        loserBet.setItems(users.stream().map(UserProfile::username).toList());
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
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public class UserSubmitDataEvent extends WaitingRoomEvent {
        private final String username;
        private final String color;
        private final InputStream photo;
        private final String userWinner;
        private final String userLoser;
    }

    @Getter
    @RequiredArgsConstructor
    @Accessors(fluent = true)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public class UserUpdatedDataEvent extends WaitingRoomEvent {
        private final String username;
        private final String color;
    }

    @Getter
    @RequiredArgsConstructor
    @Accessors(fluent = true)
    @EqualsAndHashCode(callSuper = false)
    @ToString
    public class UserBetEvent extends WaitingRoomEvent {
        private final String username;
        private final String betOn;
        private final boolean winner;
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

    public Registration addUserBetEventListener(ComponentEventListener<UserBetEvent> listener) {
        return addListener(UserBetEvent.class, listener);
    }
}
