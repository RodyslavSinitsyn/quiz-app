package org.rsinitsyn.quiz.page.cleverest;

import com.vaadin.flow.router.BeforeEnterEvent;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.page.MainPage;
import org.rsinitsyn.quiz.service.GameService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class PageValidator {

    private final GameService gameService;

    public CheckResult validate(String gameId,
                                GameStatus expectedStatus) {
        if (!validUuid(gameId) || !gameService.exist(gameId)) {
            return CheckResult.notExist();
        }
        final var game = gameService.findById(gameId);
        if (game.getStatus() == expectedStatus) {
            return CheckResult.builder()
                    .game(game)
                    .navigationRequired(false)
                    .navigateAction(Optional.empty())
                    .notificationMessage(Optional.empty())
                    .build();
        }
        return switch (game.getStatus()) {
            case NOT_STARTED -> redirect(game, e -> e.forwardTo(CleverestWaitingPage.class, gameId), "Ждем игроков");
            case STARTED -> redirect(game, e -> e.forwardTo(CleverestGamePage.class, gameId), "Игра уже идет");
            case FINISHED -> redirect(game, e -> e.forwardTo(CleverestResultsPage.class, gameId), "Игра закончена");
        };
    }

    private CheckResult redirect(
            final GameEntity game,
            final Consumer<BeforeEnterEvent> redirectAction,
            final String message
    ) {
        return CheckResult.builder()
                .game(game)
                .navigationRequired(true)
                .navigateAction(Optional.of(redirectAction))
                .notificationMessage(Optional.of(message))
                .build();
    }

    private boolean validUuid(String id) {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Builder
    public record CheckResult(GameEntity game,
                              boolean navigationRequired,
                              Optional<Consumer<BeforeEnterEvent>> navigateAction,
                              Optional<String> notificationMessage) {

        public static CheckResult notExist() {
            return CheckResult.builder()
                    .navigationRequired(true)
                    .notificationMessage(Optional.of("Такой игры нет :("))
                    .navigateAction(Optional.of(e -> e.forwardTo(MainPage.class)))
                    .build();
        }
    }
}
