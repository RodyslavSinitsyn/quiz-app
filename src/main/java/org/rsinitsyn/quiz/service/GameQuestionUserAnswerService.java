package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameParticipantDao;
import org.rsinitsyn.quiz.dao.GameQuestionDao;
import org.rsinitsyn.quiz.dao.GameQuestionUserAnswerDao;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.entity.GameQuestionUserAnswerEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserAnswerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.rsinitsyn.quiz.entity.GameParticipantId.gameParticipantId;
import static org.rsinitsyn.quiz.entity.GameQuestionId.gameQuestionId;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameQuestionUserAnswerService {

    private final GameQuestionUserAnswerDao gameQuestionUserAnswerDao;
    private final GameQuestionDao gameQuestionDao;
    private final GameParticipantDao gameParticipantDao;

    @Transactional
    public void submitAnswer(
            UUID gameId,
            UUID questionId,
            UUID userId,
            String answerText,
            AnswerStatus answerStatus,
            long responseTimeMs) {
        final var id = new GameQuestionUserAnswerId(gameId, questionId, userId);

        final var answer = gameQuestionUserAnswerDao.findById(id)
                .orElseGet(() -> {
                    final var entity = new GameQuestionUserAnswerEntity();
                    entity.setId(id);
                    entity.setGameQuestion(gameQuestionDao.getReferenceById(gameQuestionId(gameId, questionId)));
                    entity.setParticipant(gameParticipantDao.getReferenceById(gameParticipantId(gameId, userId)));
                    return entity;
                });

        answer.setAnswerText(answerText);
        answer.setAnswerStatus(answerStatus);
        answer.setResponseTimeMs(responseTimeMs);

        gameQuestionUserAnswerDao.save(answer);
    }

    @Transactional
    public void submitAnswersBatch(Collection<GameQuestionUserAnswerEntity> answers) {
        gameQuestionUserAnswerDao.saveAll(answers);
    }

    @Transactional(readOnly = true)
    public Optional<GameQuestionUserAnswerEntity> getPlayerAnswer(
            UUID gameId,
            UUID questionId,
            UUID userId) {
        return gameQuestionUserAnswerDao.findById(
                new GameQuestionUserAnswerId(gameId, questionId, userId)
        );
    }

    @Transactional(readOnly = true)
    public List<GameQuestionUserAnswerEntity> getQuestionAnswers(
            UUID gameId,
            UUID questionId) {
        return gameQuestionUserAnswerDao.findAllByIdGameIdAndIdQuestionId(gameId, questionId);
    }
}
