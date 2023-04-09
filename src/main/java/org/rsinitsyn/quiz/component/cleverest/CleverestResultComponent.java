package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.service.CleverestGameState;

public class CleverestResultComponent extends VerticalLayout {

    private VerticalLayout shortResultLayout = new VerticalLayout();
    private Grid<CleverestResultDto> grid = new Grid(CleverestResultDto.class, false);

    public CleverestResultComponent(Map<String, CleverestGameState.UserGameState> users,
                                    Map<QuizQuestionModel, List<CleverestGameState.UserGameState>> history) {
        AtomicInteger counter = new AtomicInteger(0);
        List<CleverestResultDto> results = history.entrySet()
                .stream()
                .map(e -> new CleverestResultDto(counter.getAndIncrement(), e.getKey(), e.getValue()))
                .toList();

        configureLayout(users, results);
        configureGrid(users, results);
        add(shortResultLayout, grid);
    }

    private void configureLayout(Map<String, CleverestGameState.UserGameState> users, List<CleverestResultDto> results) {
        shortResultLayout.setAlignItems(Alignment.START);
        shortResultLayout.setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        CleverestGameState.UserGameState winner = users.values()
                .stream()
                .sorted(Comparator.comparing(CleverestGameState.UserGameState::getScore).reversed())
                .limit(1)
                .findFirst().orElseThrow();
        shortResultLayout.add(new H4("Победитель"));
        shortResultLayout.add(new Span(winner.getUsername()));


        CleverestGameState.UserGameState loser = users.values()
                .stream()
                .sorted(Comparator.comparing(CleverestGameState.UserGameState::getScore))
                .limit(1)
                .findFirst().orElseThrow();
        shortResultLayout.add(new H4("Проигравший"));
        shortResultLayout.add(new Span(loser.getUsername()));

        Set<String> winnerBetWinners = users.values()
                .stream()
                .filter(uState -> uState.getWinnerBet().equals(winner.getUsername()))
                .map(CleverestGameState.UserGameState::getUsername)
                .collect(Collectors.toSet());
        shortResultLayout.add(new H4("Те кто угадал победителя"));
        winnerBetWinners.forEach(uName -> {
            shortResultLayout.add(new Span(uName));
        });

        Set<String> loserBetWinners = users.values()
                .stream()
                .filter(uState -> uState.getLoserBet().equals(loser.getUsername()))
                .map(CleverestGameState.UserGameState::getUsername)
                .collect(Collectors.toSet());
        shortResultLayout.add(new H4("Те кто угадал проигравшего"));
        loserBetWinners.forEach(uName -> {
            shortResultLayout.add(new Span(uName));
        });

        Map<String, Double> userToAvgResponseTime = results.stream()
                .flatMap(dto -> dto.getUserGameStates().stream())
                .filter(uState -> uState.getLastResponseTime() > 0)
                .collect(Collectors.groupingBy(CleverestGameState.UserGameState::getUsername,
                        Collectors.averagingLong(CleverestGameState.UserGameState::getLastResponseTime)));
        shortResultLayout.add(new H4("Среднее время ответа"));
        userToAvgResponseTime.forEach((uName, avgTime) -> {
            shortResultLayout.add(new Span(uName + " - " + String.format("%.2f", avgTime / 1000.0)));
        });
    }

    private void configureGrid(Map<String, CleverestGameState.UserGameState> users, List<CleverestResultDto> results) {
        grid.addColumn(dto -> dto.getQuestion().getText()).setHeader("#Вопрос");
        Set<String> uniqueUsernames = results.stream().flatMap(dto -> dto.getUserGameStates().stream())
                .map(CleverestGameState.UserGameState::getUsername)
                .collect(Collectors.toSet());

        List<CleverestGameState.UserGameState> anyStates = results.stream()
                .skip(results.size() - 1)
                .flatMap(dto -> dto.getUserGameStates().stream())
                .toList();

        Function<String, String> findColor = uName -> anyStates.stream()
                .filter(userGameState -> userGameState.getUsername().equals(uName))
                .map(CleverestGameState.UserGameState::getColor)
                .findFirst().orElse("");

        uniqueUsernames.forEach(username -> {
            grid.addColumn(new ComponentRenderer<>(resultDto -> {
                        var state = results.stream()
                                .filter(dto -> dto.getNumber() == resultDto.getNumber())
                                .flatMap(dto -> dto.getUserGameStates().stream())
                                .filter(us -> us.getUsername().equals(username))
                                .findFirst().orElse(null);

                        if (state == null) {
                            return new VerticalLayout(new Span("-"));
                        }

                        VerticalLayout layout = new VerticalLayout();

                        layout.add(state.isLastWasCorrect()
                                ? CleverestComponents.doneIcon() : CleverestComponents.cancelIcon());
                        layout.add(new Span("Баллы: " + state.getScore()));

                        String timeInSeconds = String.format("%.2f", state.getLastResponseTime() / 1000.0);
                        layout.add(new Span("Время: " + timeInSeconds)
                        );

                        return layout;
                    }))
                    .setHeader(username)
                    .getStyle()
                    .set("background-color", findColor.apply(username));
        });
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid.setItems(results);
    }

    @Data
    @AllArgsConstructor
    static class CleverestResultDto {
        private int number;
        private QuizQuestionModel question;
        private List<CleverestGameState.UserGameState> userGameStates;
    }
}
