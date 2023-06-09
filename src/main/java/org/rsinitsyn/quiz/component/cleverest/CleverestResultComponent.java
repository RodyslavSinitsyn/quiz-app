package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

public class CleverestResultComponent extends VerticalLayout {

    private final Grid<UserGameState> grid = new Grid<>(UserGameState.class, false);
    private final Grid<CleverestResultDto> historyGrid = new Grid<>(CleverestResultDto.class, false);

    public void setState(Collection<UserGameState> userGameStates,
                         Map<QuestionModel, List<UserGameState>> history,
                         String username) {
        AtomicInteger qNumber = new AtomicInteger(0);
        List<CleverestResultDto> results = history.entrySet()
                .stream()
                .map(e -> new CleverestResultDto(qNumber.getAndIncrement(), e.getKey(), e.getValue()))
                .toList();

        configureResultGrid(userGameStates);
        if (StringUtils.isEmpty(username)) {
            configureHistoryGrid(userGameStates.stream().map(UserGameState::getUsername).collect(Collectors.toSet()), results);
        } else {
            configureHistoryGrid(Collections.singleton(username), results);
        }
        add(QuizComponents.mainHeader("Результаты"),
                grid,
                QuizComponents.subHeader("История ответов"),
                historyGrid);
    }

    private void configureResultGrid(Collection<UserGameState> users) {
        grid.setAllRowsVisible(true);
        grid.setItems(users);
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_NO_BORDER);
        grid.addClassNames(LumoUtility.FontWeight.LIGHT,
                LumoUtility.FontSize.XXLARGE);

        grid.addColumn(UserGameState::getLastPosition)
                .setHeader("Место")
                .setFlexGrow(0);
        grid.addColumn(UserGameState::getUsername)
                .setHeader("Игрок")
                .setAutoWidth(true);
        grid.addColumn(uState -> String.format("%f", QuizUtils.divide(uState.getAvgResponseTime(), 1000)))
                .setHeader("Время на ответ");
        grid.addColumn(UserGameState::getCorrectAnswersCount)
                .setHeader("Верных ответов");
        grid.addColumn(UserGameState::getScore)
                .setHeader("Очки")
                .setFlexGrow(0);
        grid.addColumn(UserGameState::getBetScore)
                .setHeader("Ставка");
        grid.addColumn(new ComponentRenderer<>(u -> new Span(
                        u.winnerBet().getRight() ? CleverestComponents.doneIcon() : CleverestComponents.cancelIcon(),
                        u.loserBet().getRight() ? CleverestComponents.doneIcon() : CleverestComponents.cancelIcon())))
                .setHeader("Ставки")
                .setFlexGrow(0);
        grid.addColumn(UserGameState::totalScore)
                .setHeader("Общее колво очков")
                .setFlexGrow(0)
                .setClassName(LumoUtility.FontWeight.SEMIBOLD);
    }

    private void configureHistoryGrid(Set<String> users, List<CleverestResultDto> results) {
        historyGrid.setAllRowsVisible(true);
        historyGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS,
                GridVariant.LUMO_WRAP_CELL_CONTENT);
        historyGrid.setItems(results);
        historyGrid.addClassNames(LumoUtility.FontWeight.LIGHT,
                LumoUtility.FontSize.LARGE);

        historyGrid.addColumn(dto -> dto.getQuestion().getText()).setHeader("#Вопрос")
                .setFlexGrow(1);

        users.forEach(username -> {
            historyGrid.addColumn(new ComponentRenderer<>(resultDto -> {
                        var state = results.stream()
                                .filter(dto -> dto.getNumber() == resultDto.getNumber())
                                .flatMap(dto -> dto.getUserGameStates().stream())
                                .filter(us -> us.getUsername().equals(username))
                                .findFirst().orElse(null);

                        VerticalLayout layout = new VerticalLayout();
                        if (state == null) {
                            layout.add(VaadinIcon.MINUS_CIRCLE_O.create());
                            return layout;
                        }
                        layout.add(state.isLastWasCorrect()
                                ? CleverestComponents.doneIcon() : CleverestComponents.cancelIcon());
                        layout.add(new Span("Баллы: " + state.getScore()));

                        String timeInSeconds = String.format("%.2f сек.", state.getLastResponseTime() / 1000.0);
                        layout.add(new Span("Время: " + timeInSeconds));
                        layout.getStyle().set("color", state.getColor());
                        layout.getStyle().set("text-shadow", StaticValuesHolder.getFontBorder());
                        layout.setPadding(false);
                        return layout;
                    }))
                    .setHeader(username)
                    .setFlexGrow(1);
        });
    }

    @Data
    @AllArgsConstructor
    static class CleverestResultDto {
        private int number;
        private QuestionModel question;
        private List<UserGameState> userGameStates;
    }
}
