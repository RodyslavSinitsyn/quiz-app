package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.page.NewGamePage;

public class QuizGameResultComponent extends VerticalLayout {

    private QuizGameStateModel gameState;
    private GameEntity gameEntity;

    private H2 title = new H2();

    private H4 resultPercent = new H4();
    private H4 resultCount = new H4();
    private H4 reaction = new H4();
    private GameQuestionsComponent gameQuestionsComponent;
    private Button newGameButton = new Button("Новая игра");

    public QuizGameResultComponent(QuizGameStateModel gameState,
                                   GameEntity gameEntity) {
        this.gameState = gameState;
        this.gameEntity = gameEntity;
        configureComponents();
        configureGameListComponent();
        add(title, resultCount, resultPercent, reaction, gameQuestionsComponent, newGameButton);
    }

    private void configureGameListComponent() {
        gameQuestionsComponent = new GameQuestionsComponent(gameEntity, new Hr());
    }

    private void configureComponents() {
        title.setText("Результаты игрока: " + gameState.getPlayerName());
        resultPercent.setText("Процент верных ответов: " + gameState.calculateAndGetAnswersResult() + "%");
        resultCount.setText("Верных ответов: " + gameState.getAnswersStatistic());
        reaction.setText(getResultReaction());
        newGameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newGameButton.addClickListener(event -> {
            getUI().ifPresent(ui -> ui.navigate(NewGamePage.class));
            Notification.show("Результат игрока '" + gameState.getPlayerName() + "' добавлен в таблицу 'Недавние игры'",
                    3_000,
                    Notification.Position.TOP_CENTER);
        });
    }

    private String getResultReaction() {
        int res = gameState.calculateAndGetAnswersResult();
        String reaction = "";
        if (res >= 90) {
            reaction = "Великолепно!";
        } else if (res >= 75) {
            reaction = "Достойно)";
        } else if (res >= 50) {
            reaction = "Так себе результат :/";
        } else if (res >= 25) {
            reaction = "Плохо...";
        } else {
            reaction = "Ты полное днище :))))00))";
        }
        return reaction;
    }
}
