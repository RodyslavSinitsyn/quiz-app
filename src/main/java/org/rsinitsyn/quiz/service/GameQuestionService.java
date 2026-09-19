package org.rsinitsyn.quiz.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameQuestionDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionId;
import org.rsinitsyn.quiz.entity.GameQuestionMetadata;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameQuestionService {

    private final GameQuestionDao gameQuestionDao;
    private final GameDao gameDao;
    private final QuestionDao questionDao;

    @Transactional
    public void addQuestionToGame(UUID gameId, UUID questionId, GameQuestionMetadata metadata) {
        final var id = new GameQuestionId(gameId, questionId);
        if (gameQuestionDao.existsById(id)) {
            log.debug("Question {} is already added to game {}", questionId, gameId);
            return;
        }
        final var game = gameDao.getReferenceById(gameId);
        final var question = questionDao.getReferenceById(questionId);

        final var entity = GameQuestionEntity.builder()
                .id(id)
                .game(game)
                .question(question)
                .metadata(metadata)
                .build();
        gameQuestionDao.save(entity);
        log.debug("Question {} added to game {}", questionId, gameId);
    }

    @Transactional
    public void removeQuestionFromGame(UUID gameId, UUID questionId) {
        final var id = new GameQuestionId(gameId, questionId);
        if (!gameQuestionDao.existsById(id)) {
            return;
        }
        gameQuestionDao.deleteById(id);
        log.debug("Question {} removed from game {}", questionId, gameId);
    }
}
