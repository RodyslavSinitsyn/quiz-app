package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.UUID;
import org.rsinitsyn.quiz.component.MainLayout;

@Route(value = "/", layout = MainLayout.class)
@PageTitle("Game")
public class GameTypePage extends VerticalLayout {

    H2 title = new H2("Новая игра");
    Button configButton;
    Select<String> gameTypeSelect;

    public GameTypePage() {
        setSizeFull();
        configButton = createNextStepButton();
        gameTypeSelect = createGameTypeSelect();
        add(title, gameTypeSelect, configButton);
    }

    private Button createNextStepButton() {
        Button button = new Button("Перейти к настройкам");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.setEnabled(false);
        button.addClickListener(event -> getUI()
                .ifPresent(ui -> ui.navigate(
                        GamePage.class,
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
        select.setItemEnabledProvider(item -> !"Своя Игра".equals(item));
        select.addValueChangeListener(event -> configButton.setEnabled(true));
        return select;
    }
}
