package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.custom.GameListGrid;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.utils.QuizComponents;

import java.util.Collections;
import java.util.function.Consumer;

@Route(value = "/", layout = MainLayout.class)
@PageTitle("Game")
@PermitAll
public class NewGamePage extends VerticalLayout {

    private static final String FONT_SIZE = LumoUtility.FontSize.XLARGE;

    private final GameService gameService;

    private GameListGrid gameListGrid = new GameListGrid(Collections.emptyList());

    public NewGamePage(GameService gameService) {
        this.gameService = gameService;
        setSizeFull();
        configureGameList();
        add(QuizComponents.mainHeader("Новая игра"),
                createPlayGameLayout(),
                QuizComponents.subHeader("Недавние игры"),
                gameListGrid);
    }

    private void configureGameList() {
        gameListGrid = new GameListGrid(gameService.findAllNewFirst());
        gameListGrid.setSizeFull();
    }

    private VerticalLayout createPlayGameLayout() {
        var layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        layout.add(
                createPlayGameButton("Викторина", ui -> ui.navigate(QuizGamePage.class)),
                createPlayGameButton("Самый умный", ui -> ui.navigate(CleverestGamePage.class))
        );
        return layout;
    }

    private Button createPlayGameButton(String gameName, Consumer<UI> onClickGoTo) {
        Button button = new Button(gameName);
        button.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_LARGE);
        button.addClassNames(FONT_SIZE);
        button.addClickListener(event -> {
            onClickGoTo.accept(event.getSource().getUI().orElseThrow());
        });
        return button;
    }
}
