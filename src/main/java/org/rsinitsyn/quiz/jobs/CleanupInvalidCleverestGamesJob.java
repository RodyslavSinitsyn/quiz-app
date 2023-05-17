package org.rsinitsyn.quiz.jobs;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(value = "quiz.job.cleanupCleverestGames", havingValue = "true")
@RequiredArgsConstructor
public class CleanupInvalidCleverestGamesJob {

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;

    @Scheduled(timeUnit = TimeUnit.SECONDS,
            initialDelay = 60,
            fixedDelay = 600)
    public void runJob() {
        List<GameEntity> games = gameService.findAllNewFirst().stream()
                .filter(e -> e.getType().equals(GameType.CLEVEREST)
                        && e.getStatus().equals(GameStatus.STARTED))
                .filter(e -> broadcaster.getState(e.getId().toString()) == null)
                .toList();
        log.debug("Started games without state count: {}", games.size());
        if (!games.isEmpty()) {
            gameService.deleteAllBatch(games);
        }
    }
}
