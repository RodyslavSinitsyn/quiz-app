package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Collections;
import java.util.UUID;
import org.rsinitsyn.quiz.component.GameListComponent;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.service.GameService;

@Route(value = "/", layout = MainLayout.class)
@PageTitle("Game")
public class NewGamePage extends VerticalLayout {

    private H2 title = new H2("Новая игра");
    private Button configButton;
    private Select<String> gameTypeSelect;
    private H3 gameListTitle = new H3("Недавние игры");
    private GameListComponent gameListComponent = new GameListComponent(Collections.emptyList());

    private GameService gameService;

    public NewGamePage(GameService gameService) {
        this.gameService = gameService;
        setSizeFull();
        configButton = createNextStepButton();
        gameTypeSelect = createGameTypeSelect();
        configureGameList();
        add(title, gameTypeSelect, configButton,
                gameListTitle, gameListComponent);
    }

    private void configureGameList() {
        gameListComponent = new GameListComponent(gameService.findAllFinishedNewFirst());
        gameListComponent.setSizeFull();
    }

    private Button createNextStepButton() {
        Button button = new Button("Играть");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> getUI()
                .ifPresent(ui -> ui.navigate(
                        QuizGamePage.class,
                        UUID.randomUUID().toString())));
        return button;
    }

    private Select<String> createGameTypeSelect() {
        Select<String> select = new Select<>();
        select.addFocusShortcut(Key.ENTER, KeyModifier.ALT);
        select.setAutofocus(true);
        select.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.FontWeight.BOLD);
        select.setItems(
                "Викторина",
                "Своя Игра");
        select.setPlaceholder("Тип игры");
        select.setValue("Викторина");
        select.setItemEnabledProvider(item -> !"Своя Игра".equals(item));
        return select;
    }
}
