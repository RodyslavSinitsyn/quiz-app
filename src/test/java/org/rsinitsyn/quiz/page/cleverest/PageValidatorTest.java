package org.rsinitsyn.quiz.page.cleverest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.service.GameService;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PageValidatorTest {

    private GameService gameService;
    private PageValidator pageValidator;

    @BeforeEach
    void setUp() {
        gameService = mock(GameService.class);
        pageValidator = new PageValidator(gameService);
    }

    @Test
    void returns_not_exist_given_invalid_uuid() {
        final var result = pageValidator.validate("invalid", GameStatus.NOT_STARTED);

        assertThat(result.navigationRequired()).isTrue();
        assertThat(result.notificationMessage()).contains("Такой игры нет :(");
        assertThat(result.navigateAction()).isPresent();
        assertThat(result.game()).isNull();
    }

    @Test
    void returns_not_exist_given_game_not_exist() {
        final var gameId = randomUUID().toString();
        given(gameService.exist(gameId)).willReturn(false);

        final var result = pageValidator.validate(gameId, GameStatus.NOT_STARTED);

        assertThat(result.navigationRequired()).isTrue();
        assertThat(result.notificationMessage()).contains("Такой игры нет :(");
        assertThat(result.navigateAction()).isPresent();
        assertThat(result.game()).isNull();
    }

    @ParameterizedTest
    @EnumSource(GameStatus.class)
    void returns_without_navigation_given_status_matched(GameStatus status) {
        final var gameId = randomUUID().toString();
        final var game = aGameEntity(status);

        given(gameService.exist(gameId)).willReturn(true);
        given(gameService.findById(gameId)).willReturn(game);

        final var result = pageValidator.validate(gameId, status);

        assertThat(result.navigationRequired()).isFalse();
        assertThat(result.notificationMessage()).isEmpty();
        assertThat(result.navigateAction()).isEmpty();
        assertThat(result.game()).isEqualTo(game);
    }

    @ParameterizedTest
    @EnumSource(GameStatus.class)
    void returns_navigation_given_game_status_not_matched(GameStatus actualStatus) {
        final var requestedStatus = switch (actualStatus) {
            case NOT_STARTED -> GameStatus.STARTED;
            case STARTED -> GameStatus.FINISHED;
            case FINISHED -> GameStatus.NOT_STARTED;
        };

        final var gameId = randomUUID().toString();
        final var game = aGameEntity(actualStatus);

        given(gameService.exist(gameId)).willReturn(true);
        given(gameService.findById(gameId)).willReturn(game);

        final var result = pageValidator.validate(gameId, requestedStatus);

        assertThat(result.navigationRequired()).isTrue();
        assertThat(result.notificationMessage()).isPresent();
        assertThat(result.navigateAction()).isPresent();
        assertThat(result.game()).isEqualTo(game);
    }

    private static GameEntity aGameEntity(GameStatus status) {
        final var gameEntity = new GameEntity();
        gameEntity.setStatus(status);
        return gameEntity;
    }
}