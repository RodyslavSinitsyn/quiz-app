package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.rsinitsyn.quiz.utils.ThemeUtils.BLACK_COLOR;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestoreStateService {

    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;
    private final QuestionService questionService;

    public void restore() {
        gameService.findAllNewFirst().stream()
                .filter(g -> g.getStatus() != GameStatus.FINISHED)
                .map(g -> gameService.findById(g.getId().toString()))
                .forEach(this::restoreGame);
    }

    @Transactional(readOnly = true)
    private void restoreGame(GameEntity gameEntity) {
        final var gameId = gameEntity.getId().toString();
        if (broadcaster.stateExists(gameId)) {
            return;
        }
        log.info("Game {} [{}] restoring state", gameEntity.getType(), gameId);
        final var questionModels = gameEntity.getGameQuestions().stream()
                .map(GameQuestionUserEntity::getQuestion)
                .distinct()
                .map(questionService::toQuizQuestionModel)
                .toList();
        broadcaster.createState(gameId,
                gameEntity.getCreatedBy(),
                questionModels,
                List.of(),
                List.of());
        log.info("Game {} [{}] questions count [{}]", gameEntity.getType(), gameId, questionModels.size());
        final var users = gameEntity.getGameQuestions().stream()
                .map(GameQuestionUserEntity::getUser)
                .filter(u -> !u.getUsername().equals(gameEntity.getCreatedBy()))
                .toList();
        log.info("Game {} [{}] users count [{}]", gameEntity.getType(), gameId, users.size());
        users.forEach(userEntity ->
                broadcaster.getState(gameId)
                        .addOrUpdateUser(gameId, userEntity.getUsername(), BLACK_COLOR, userEntity.getPhotoFilename(), "", ""));
        log.info("Game {} [{}] state restored", gameEntity.getType(), gameId);
    }
}
