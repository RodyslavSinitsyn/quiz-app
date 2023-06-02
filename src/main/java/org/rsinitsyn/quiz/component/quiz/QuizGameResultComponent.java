package org.rsinitsyn.quiz.component.quiz;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.rsinitsyn.quiz.component.сustom.GameResultsComponent;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.page.NewGamePage;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class QuizGameResultComponent extends VerticalLayout {

    private QuizGameState gameState;
    private GameEntity gameEntity;

    private H5 title = new H5();
    private H5 resultPercent = new H5();
    private H5 resultCount = new H5();
    private H5 reaction = new H5();
    private Grid<CategoryResultDto> categoryDetailsGrid = new Grid<>(CategoryResultDto.class, false);
    private GameResultsComponent gameResultsComponent;
    private Button newGameButton = new Button("Новая игра");

    public QuizGameResultComponent(QuizGameState gameState,
                                   GameEntity gameEntity) {
        this.gameState = gameState;
        this.gameEntity = gameEntity;
        configureComponents();
        configureGrid();
        configureGameListComponent();
        add(resultCount, resultPercent, reaction, categoryDetailsGrid, gameResultsComponent, newGameButton);
    }

    private void configureGrid() {
        List<CategoryResultDto> gridItems = gameEntity.getGameQuestions()
                .stream()
                .collect(Collectors.groupingBy(gqe -> gqe.getQuestion().getCategory().getName()))
                .entrySet()
                .stream()
                .map(entry -> new CategoryResultDto(
                        entry.getKey(),
                        (int) entry.getValue().stream().filter(GameQuestionUserEntity::getAnswered).count(),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(CategoryResultDto::getAnswersRate, Comparator.reverseOrder()))
                .toList();

        categoryDetailsGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
        categoryDetailsGrid.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD);
        categoryDetailsGrid.setWidth("30em");
        categoryDetailsGrid.setAllRowsVisible(true);
        categoryDetailsGrid.setItems(gridItems);
        categoryDetailsGrid.addColumn(CategoryResultDto::getCategoryName).setHeader("Тема").setFlexGrow(2);
        categoryDetailsGrid.addColumn(dto -> dto.getCorrectAnswersCount() + "/" + dto.getTotalAnswersCount()).setHeader("Ответы");
        categoryDetailsGrid.addColumn(dto -> dto.getAnswersRate() + "%").setHeader("Процент");
    }

    private void configureGameListComponent() {
        gameResultsComponent = new GameResultsComponent(gameEntity, new Hr());
    }

    private void configureComponents() {
        title.setText("Результаты игрока: " + gameState.getPlayerName());
        resultPercent.setText("Процент верных ответов: " + gameState.calculateAndGetAnswersResult() + "%");
        resultCount.setText("Верных ответов: " + gameState.getAnswersStatistic());
        reaction.setText(getResultReaction());
        newGameButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newGameButton.addClickListener(event -> {
            getUI().ifPresent(ui -> ui.navigate(NewGamePage.class));
            QuizComponents.infoNotification(
                    "Результат игрока '" + gameState.getPlayerName() + "' добавлен в таблицу 'Недавние игры'");
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

    @Data
    @AllArgsConstructor
    private static class CategoryResultDto {
        private String categoryName;
        private int correctAnswersCount;
        private int totalAnswersCount;

        public int getAnswersRate() {
            return (int) QuizUtils.divide(correctAnswersCount * 100, totalAnswersCount);
        }
    }
}
