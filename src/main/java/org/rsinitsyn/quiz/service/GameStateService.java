package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameParticipantDao;
import org.rsinitsyn.quiz.dao.GameQuestionDao;
import org.rsinitsyn.quiz.dao.GameQuestionUserAnswerDao;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.rsinitsyn.quiz.entity.GameParticipantId.gameParticipantId;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameStateService {

    private final GameDao gameDao;
    private final GameParticipantDao gameParticipantDao;
    private final GameQuestionDao gameQuestionDao;
    private final GameQuestionUserAnswerDao gameQuestionUserAnswerDao;
    private final QuestionService questionService;

    @Transactional(readOnly = true)
    public QuizGameState restoreQuizGameState(UUID gameId, UUID userId) {
        final var maybeGameEntity = gameDao.findById(gameId);
        if (maybeGameEntity.isEmpty()) {
            throw new IllegalArgumentException("Game %s not found".formatted(gameId));
        }

        final var gamePlayer = gameParticipantDao.findById(gameParticipantId(gameId, userId));
        if (gamePlayer.isEmpty()) {
            throw new IllegalArgumentException("User %s is not a participant of game %s".formatted(userId, gameId));
        }

        final var gameEntity = maybeGameEntity.get();
        var state = new QuizGameState();
        state.setGameId(gameEntity.getId());
        state.setGameName(gameEntity.getName());
        state.setStatus(gameEntity.getStatus());

        state.setPlayerName(gamePlayer.get().getUser().getUsername());
        state.setPlayerId(gamePlayer.get().getUser().getId());

        final var gameQuestions = gameQuestionDao.findByGameId(gameId);

        state.setQuestions(gameQuestions.stream()
                // todo: how to handle sorting by details order id?
//                .sorted(Comparator.comparing(GameQuestionUserEntity::getOrderNumber, Comparator.naturalOrder()))
                .map(GameQuestionEntity::getQuestion)
                .map(questionService::toQuizQuestionModel)
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        final var playerAnswers = gameQuestionUserAnswerDao.findAllByIdGameIdAndIdUserId(gameId, userId);

        playerAnswers.stream()
                .filter(a -> a.getStatus().correct())
                .forEach(ignored -> state.incrementCorrectAnswersCounter());

        final var answeredQuestionIds = playerAnswers.stream()
                .filter(answer -> answer.getStatus().answered())
                .map(answer -> answer.getId().getQuestionId())
                .collect(Collectors.toSet());
        state.setCurrentQuestionNumber(Math.min(answeredQuestionIds.size(), gameQuestions.size()));

        final var configuration = gameEntity.getConfiguration();
        state.setAnswerOptionsEnabled(configuration.optionsEnabled());
        state.setTimerEnabled(configuration.questionTime() != null);
        state.setHintsEnabled(configuration.hintsEnabled());
        state.setIntrigueEnabled(configuration.intrigueEnabled());

        return state;
    }

    @Deprecated(forRemoval = true)
    @Transactional(readOnly = true)
    public QuizGameState restoreQuizGameStateOld(String gameId) {
        var gameEntity = findById(gameId);
        var gameQuestions = gameEntity.getGameQuestions();
        var state = new QuizGameState();
        state.setGameId(gameEntity.getId());
        state.setGameName(gameEntity.getName());
        state.setPlayerName(gameEntity.getPlayerNames().stream().findFirst().orElseThrow());
        state.setAnswerOptionsEnabled(true);
        state.setQuestions(gameQuestions.stream()
                .sorted(Comparator.comparing(GameQuestionUserEntity::getOrderNumber, Comparator.naturalOrder()))
                .map(GameQuestionUserEntity::getQuestion)
                .map(questionService::toQuizQuestionModel)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        gameQuestions.stream()
                .filter(e -> Boolean.TRUE.equals(e.getAnswered()))
                .forEach(e -> {
                    state.incrementCorrectAnswersCounter();
                });
        state.setStatus(gameEntity.getStatus());
        var currentQuestionNumber = (int) (gameQuestions.size() - gameQuestions
                .stream()
                .filter(e -> e.getAnswered() == null)
                .count());
        state.setCurrentQuestionNumber(Math.max(0, currentQuestionNumber));
        return state;
    }

    @Transactional(readOnly = true)
    public GameEntity findById(String id) {
        return gameDao.findByIdJoinQuestions(UUID.fromString(id)).stream()
                .peek(gq -> {
                    gq.getGameQuestions().stream()
                            .map(GameQuestionUserEntity::getQuestion)
                            .forEach(q -> {
                                Hibernate.initialize(q.getAnswers());
                                Hibernate.initialize(q.getGrades());
                                Hibernate.initialize(q.getHints());
                            });
                })
                .findFirst()
                .orElse(null);
    }
}
