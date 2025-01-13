package org.rsinitsyn.quiz.service;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameQuestionUserDao;
import org.rsinitsyn.quiz.entity.*;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Observed(name = "gameService")
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
    private final GameDao gameDao;
    private final GameQuestionUserDao gameQuestionUserDao;
    private final QuestionService questionService;
    private final UserService userService;

    public GameEntity findById(String id) {
        return gameDao.findByIdJoinQuestions(UUID.fromString(id))
                .orElse(null);
    }

    public boolean exists(UUID id) {
        return gameDao.existsById(id);
    }

    @Transactional
    public void submitAnswersBatch(String gameId, QuestionModel question, List<UserGameState> userStates) {
        userStates.forEach(userGameState -> {
            submitAnswers(
                    gameId,
                    userGameState.getUsername(),
                    question,
                    Collections.singletonList(userGameState.getLastAnswerText()),
                    () -> userGameState.isAnswerGiven() ? userGameState.isLastWasCorrect() : null);
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void submitAnswers(String gameId,
                              String playerName,
                              QuestionModel questionModel,
                              List<String> answersList,
                              Supplier<Boolean> correctAnswerProvider) {
        UserEntity user = userService.findByUsername(playerName);
        var primaryKey = new GameQuestionUserId(
                UUID.fromString(gameId),
                questionModel.getId(),
                user.getId());
        Optional<GameQuestionUserEntity> optEntity = gameQuestionUserDao.findById(primaryKey);
        if (optEntity.isPresent()) {
            GameQuestionUserEntity persistent = optEntity.get();
            persistent.setAnswered(correctAnswerProvider.get());
            persistent.setAnswerText(String.join(",", answersList));

            gameQuestionUserDao.save(persistent);
        } else {
            GameQuestionUserEntity newEntity = new GameQuestionUserEntity();
            newEntity.setId(primaryKey);
            newEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
            newEntity.setUser(user);
            newEntity.setGame(findById(gameId));
            newEntity.setAnswered(correctAnswerProvider.get());
            newEntity.setAnswerText(String.join(",", answersList));
            newEntity.setOrderNumber(gameQuestionUserDao.getMaxOrderNumber(primaryKey.getGameId()) + 1);
            gameQuestionUserDao.save(newEntity);
        }
    }

    public boolean createIfNotExists(String id, GameType gameType) {
        Optional<GameEntity> gameEntityOptional = gameDao.findById(UUID.fromString(id));
        if (gameEntityOptional.isPresent()) {
            log.info("Game already exists, id: {}", id);
            return false;
        }
        GameEntity entity = new GameEntity();
        entity.setId(UUID.fromString(id));
        entity.setName("Cleverest");
        entity.setStatus(GameStatus.NOT_STARTED);
        entity.setType(gameType);
        entity.setCreatedBy(SessionWrapper.getLoggedUser());
        entity.setCreationDate(LocalDateTime.now());
        GameEntity saved = gameDao.save(entity);
        log.info("Game created, id: {}", saved.getId());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void update(String id, String name, GameStatus status) {
        GameEntity gameEntity = gameDao.findById(UUID.fromString(id)).orElseThrow();
        setNewFields(gameEntity, name, status);
        log.info("Updating game, id: {}", id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void linkQuestionsWithGame(String id, QuizGameState stateModel) {
        GameEntity gameEntity = findById(id);
        setNewFields(gameEntity, stateModel.getGameName(), GameStatus.NOT_STARTED);
        log.info("Updating game, id: {}", id);

        UserEntity user = userService.findByUsername(stateModel.getPlayerName());
        AtomicInteger questionOrder = new AtomicInteger(0);
        List<GameQuestionUserEntity> gameQuestionEntitiesToSave = stateModel.getQuestions().stream().map(questionModel -> {
            GameQuestionUserEntity gameQuestionUserEntity = new GameQuestionUserEntity();
            gameQuestionUserEntity.setId(new GameQuestionUserId(
                    UUID.fromString(id),
                    questionModel.getId(),
                    user.getId()));
            gameQuestionUserEntity.setAnswered(null);
            gameQuestionUserEntity.setOrderNumber(questionOrder.getAndIncrement());
            gameQuestionUserEntity.setGame(gameEntity);
            gameQuestionUserEntity.setUser(user);
            gameQuestionUserEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
            return gameQuestionUserEntity;
        }).toList();

        List<GameQuestionUserEntity> savedGameQuestions = gameQuestionUserDao.saveAll(gameQuestionEntitiesToSave);
        log.info("Saved game questions, size: {}", savedGameQuestions.size());
    }

    private void setNewFields(GameEntity gameEntity, String name, GameStatus status) {
        gameEntity.setName(name);
        gameEntity.setCreatedBy(SessionWrapper.getLoggedUser());
        gameEntity.setStatus(status);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void linkQuestionsAndUsersWithGame(String gameId,
                                              Set<String> usernames,
                                              List<QuestionModel> questions) {
        GameEntity gameEntity = findById(gameId);

        Collection<GameQuestionUserEntity> entities = new ArrayList<>();
        usernames.forEach(username -> {
            UserEntity user = userService.findByUsername(username);

            AtomicInteger qCounter = new AtomicInteger(0);
            questions.forEach(questionModel -> {
                GameQuestionUserEntity gameQuestionUserEntity = new GameQuestionUserEntity();
                var pk = new GameQuestionUserId(
                        gameEntity.getId(),
                        questionModel.getId(),
                        user.getId());
                gameQuestionUserEntity.setId(pk);
                gameQuestionUserEntity.setGame(gameEntity);
                gameQuestionUserEntity.setUser(user);
                gameQuestionUserEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
                gameQuestionUserEntity.setOrderNumber(qCounter.getAndIncrement());
                gameQuestionUserEntity.setAnswered(null);

                entities.add(gameQuestionUserEntity);
            });
        });

        gameQuestionUserDao.saveAll(entities);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void finishGame(String id) {
        gameDao.findById(UUID.fromString(id))
                .ifPresent(gameEntity -> {
                    gameEntity.setStatus(GameStatus.FINISHED);
                    gameEntity.setFinishDate(LocalDateTime.now());
                });
    }

    public List<GameEntity> findAllNewFirst() {
        return gameDao.findAllJoinGamesQuestionsNewFirst();
    }

    @Transactional(readOnly = true)
    public QuizGameState getQuizGameState(String gameId) {
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
                .collect(Collectors.toSet()));
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

    @Transactional
    public void deleteAllBatch(Collection<GameEntity>... gameEntities) {
        Arrays.stream(gameEntities).forEach(gameDao::deleteAll);
        log.debug("Games deleted, size: {}", Arrays.stream(gameEntities).mapToLong(Collection::size).sum());
    }
}
