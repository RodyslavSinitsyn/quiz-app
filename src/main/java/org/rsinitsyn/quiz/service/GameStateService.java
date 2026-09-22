package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameParticipantDao;
import org.rsinitsyn.quiz.dao.GameQuestionDao;
import org.rsinitsyn.quiz.dao.GameQuestionUserAnswerDao;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static org.rsinitsyn.quiz.entity.GameParticipantId.gameParticipantId;
import static org.rsinitsyn.quiz.entity.GameParticipantRole.PLAYER;
import static org.rsinitsyn.quiz.utils.ThemeUtils.BLACK_COLOR;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameStateService {

    private final GameDao gameDao;
    private final GameParticipantDao gameParticipantDao;
    private final GameQuestionDao gameQuestionDao;
    private final GameQuestionUserAnswerDao gameQuestionUserAnswerDao;
    private final QuestionService questionService;
    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;

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

    @Deprecated()
    @Transactional(readOnly = true)
    public QuizGameState restoreQuizGameStateOld(String gameId) {
        var gameEntity = gameDao.findById(UUID.fromString(gameId)).orElseThrow();
        var gameQuestions = gameEntity.getGameQuestions();
        var state = new QuizGameState();
        state.setGameId(gameEntity.getId());
        state.setGameName(gameEntity.getName());
        state.setPlayerName(gameEntity.getPlayerNames().stream().findFirst().orElseThrow());
        state.setAnswerOptionsEnabled(true);
        state.setQuestions(gameQuestions.stream()
                .sorted(comparing(GameQuestionUserEntity::getOrderNumber, Comparator.naturalOrder()))
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

    public void restoreAllCleverest() {
        gameDao.findAll().stream()
                .filter(g -> g.getStatus() != GameStatus.FINISHED)
                .forEach(this::restoreCleverestGameState);
    }

    @Transactional(readOnly = true)
    private void restoreCleverestGameState(GameEntity gameEntity) {
        final var gameId = gameEntity.getId();

        if (broadcaster.stateExists(gameId.toString())) {
            return;
        }

        final var gameQuestions = gameQuestionDao.findByGameId(gameId);

        final var firstRound = toQuestions(gameQuestions, 1);
        final var secondRound = toQuestions(gameQuestions, 2);
        final var thirdRound = toQuestions(gameQuestions, 3);

        final var state = new CleverestGameState(
                gameEntity.getCreatedBy(),
                firstRound,
                secondRound,
                thirdRound
        );

        final var players = gameParticipantDao.findByGameIdAndRole(gameId, PLAYER);

        players.forEach(participant -> {
            final var user = participant.getUser();
            state.addOrUpdateUser(
                    user.getId(),
                    user.getUsername(),
                    BLACK_COLOR,
                    user.getPhotoFilename(),
                    "",
                    "");
        });

        broadcaster.restoreState(gameId.toString(), state);

        log.debug("Game {} [{}] state restored: questions={}, users={}",
                gameEntity.getType(),
                gameId,
                gameQuestions.size(),
                state.getAllUsernames().size());
    }

    private List<QuestionModel> toQuestions(List<GameQuestionEntity> gameQuestions,
                                            int round) {
        return gameQuestions.stream()
                .filter(q -> q.getMetadata().round() == round)
                .sorted(comparing(q -> q.getMetadata().order()))
                .map(GameQuestionEntity::getQuestion)
                .map(questionService::toQuizQuestionModel)
                .toList();
    }

    @Deprecated
    @Transactional(readOnly = true)
    private void restoreCleverestGameStateOld(GameEntity gameEntity) {
        final var gameId = gameEntity.getId().toString();
        if (broadcaster.stateExists(gameId)) {
            return;
        }
        log.debug("Game {} [{}] restoring state", gameEntity.getType(), gameId);
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
        log.debug("Game {} [{}] questions count [{}]", gameEntity.getType(), gameId, questionModels.size());
        final var users = gameEntity.getGameQuestions().stream()
                .map(GameQuestionUserEntity::getUser)
                .filter(u -> !u.getUsername().equals(gameEntity.getCreatedBy()))
                .toList();
        log.debug("Game {} [{}] users count [{}]", gameEntity.getType(), gameId, users.size());
        users.forEach(userEntity ->
                broadcaster.getState(gameId)
                        .addOrUpdateUser(null, userEntity.getUsername(), BLACK_COLOR, userEntity.getPhotoFilename(), "", ""));
        log.debug("Game {} [{}] state restored", gameEntity.getType(), gameId);
    }

}
