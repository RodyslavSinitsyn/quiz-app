package org.rsinitsyn.quiz.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
    private final GameDao gameDao;

    public void createIfNotExists(String id) {
        Optional<GameEntity> gameEntityOptional = gameDao.findById(UUID.fromString(id));
        if (gameEntityOptional.isPresent()) {
            log.info("Game already exists, id: {}", id);
            return;
        }
        GameEntity entity = new GameEntity();
        entity.setId(UUID.fromString(id));
        entity.setStatus(GameStatus.NOT_STARTED);
        entity.setType(GameType.QUIZ); // TODO Obrain from UI
        entity.setCreationDate(LocalDateTime.now());
        GameEntity saved = gameDao.save(entity);
        log.info("Game created, id: {}", saved.getId());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void update(String id, String name, String createdBy, GameStatus status, Integer questionsCount, Integer percentageResult) {
        gameDao.findById(UUID.fromString(id))
                .ifPresent(gameEntity -> {
                    gameEntity.setName(name);
                    gameEntity.setCreatedBy(createdBy);
                    gameEntity.setStatus(status);
                    gameEntity.setQuestionsCount(questionsCount);
                    gameEntity.setResult(percentageResult);
                });
        log.info("Updating game, id: {}", id);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void finish(String id, QuizGameStateModel quizGameStateModel) {
        gameDao.findById(UUID.fromString(id))
                .ifPresent(gameEntity -> {
                    gameEntity.setStatus(GameStatus.FINISHED);
                    gameEntity.setResult(quizGameStateModel.calculateAndGetResult());
                });
    }

}
