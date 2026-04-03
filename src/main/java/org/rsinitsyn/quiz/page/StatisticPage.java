package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.model.UserStatsModel;
import org.rsinitsyn.quiz.service.StatisticService;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.SessionWrapper;

import java.util.List;

@Route(value = "/statistic", layout = MainLayout.class)
@PageTitle("Statistic")
@PermitAll
public class StatisticPage extends VerticalLayout {

    private StatisticService statisticService;
    private Grid<UserStatsModel> grid = new Grid<>(UserStatsModel.class, false);

    public StatisticPage(StatisticService statisticService) {
        this.statisticService = statisticService;
        configureGrid();
        add(QuizComponents.mainHeader("Статистика игроков"), grid);
    }

    private void configureGrid() {
        List<UserStatsModel> stats = statisticService.getAllUsersStats();
        grid.setSizeFull();
        grid.setAllRowsVisible(true);
        grid.setItems(stats);
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT, GridVariant.LUMO_COLUMN_BORDERS);
        grid.addColumn(UserStatsModel::getUsername).setHeader("Имя");
        grid.addColumn(UserStatsModel::getQuestionsCreated).setHeader("Вопросов создано");
        grid.addColumn(UserStatsModel::getGamesCreated).setHeader("Игр создано");
        grid.addColumn(UserStatsModel::getGamesPlayed).setHeader("Игр сыграно");
        grid.addColumn(UserStatsModel::getAnswersStats).setHeader("Ответы");
        grid.addColumn(UserStatsModel::getCorrectAnswersRate).setHeader("Процент ответов");

        stats.stream().filter(m -> m.getUsername().equals(SessionWrapper.getLoggedUser())).findFirst()
                .ifPresent(userStatsModel -> grid.select(userStatsModel));
    }
}
