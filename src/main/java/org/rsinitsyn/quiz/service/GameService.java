package org.rsinitsyn.quiz.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameQuestionUserDao;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserPrimaryKey;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
    private final GameDao gameDao;
    private final GameQuestionUserDao gameQuestionUserDao;
    private final QuestionService questionService;
    private final UserService userService;

    public GameEntity findById(String id) {
        return gameDao.findById(UUID.fromString(id))
                .orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void submitAnswers(String gameId, String playerName, QuizQuestionModel questionModel, Set<QuizQuestionModel.QuizAnswerModel> userAnswers) {
        UserEntity user = userService.findByUsername(playerName);
        var primaryKey = new GameQuestionUserPrimaryKey(
                UUID.fromString(gameId),
                questionModel.getId(),
                user.getId());
        Optional<GameQuestionUserEntity> optEntity = gameQuestionUserDao.findById(primaryKey);
        if (optEntity.isPresent()) {
            GameQuestionUserEntity persistent = optEntity.get();
            persistent.setAnswered(questionModel.areAnswersCorrect(userAnswers));

            gameQuestionUserDao.save(persistent);
        } else {
            GameQuestionUserEntity newEntity = new GameQuestionUserEntity();
            newEntity.setId(primaryKey);
            newEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
            newEntity.setUser(user);
            newEntity.setGame(findById(gameId));
            newEntity.setAnswered(questionModel.areAnswersCorrect(userAnswers));
            newEntity.setOrderNumber(0);

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
        entity.setStatus(GameStatus.NOT_STARTED);
        entity.setType(gameType);
        entity.setCreatedBy(QuizUtils.getLoggedUser());
        entity.setCreationDate(LocalDateTime.now());
        GameEntity saved = gameDao.save(entity);
        log.info("Game created, id: {}", saved.getId());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void update(String id, String name, String playerName, GameStatus status, Integer questionsCount, Integer percentageResult) {
        GameEntity gameEntity = findById(id);
        setNewFields(gameEntity, name, playerName, status, questionsCount, percentageResult);
        log.info("Updating game, id: {}", id);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void linkQuestionsWithGame(String id, QuizGameStateModel stateModel) {
        GameEntity gameEntity = findById(id);
        setNewFields(gameEntity, stateModel.getGameName(), stateModel.getPlayerName(), GameStatus.STARTED, stateModel.getQuestions().size(), stateModel.calculateAndGetAnswersResult());
        log.info("Updating game, id: {}", id);

        UserEntity user = userService.findByUsername(stateModel.getPlayerName());
        AtomicInteger questionOrder = new AtomicInteger(0);
        List<GameQuestionUserEntity> gameQuestionEntitiesToSave = stateModel.getQuestions().stream().map(questionModel -> {
            GameQuestionUserEntity gameQuestionUserEntity = new GameQuestionUserEntity();
            gameQuestionUserEntity.setId(new GameQuestionUserPrimaryKey(
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

    private void setNewFields(GameEntity gameEntity, String name, String playerName, GameStatus status, Integer questionsCount, Integer percentageResult) {
        gameEntity.setName(name);
        gameEntity.setCreatedBy(QuizUtils.getLoggedUser());
        gameEntity.setPlayerName(playerName);
        gameEntity.setStatus(status);
        gameEntity.setQuestionsCount(questionsCount);
        gameEntity.setResult(percentageResult);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void linkQuestionsAndUsersWithGame(String gameId,
                                              Set<String> usernames,
                                              Set<QuizQuestionModel> questions) {
        GameEntity gameEntity = findById(gameId);

        Collection<GameQuestionUserEntity> entities = new ArrayList<>();
        usernames.forEach(username -> {
            UserEntity user = userService.findByUsername(username);

            AtomicInteger qCounter = new AtomicInteger(0);
            questions.forEach(questionModel -> {
                GameQuestionUserEntity gameQuestionUserEntity = new GameQuestionUserEntity();
                var pk = new GameQuestionUserPrimaryKey(
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
    public void finishQuizGame(String id, QuizGameStateModel quizGameStateModel) {
        gameDao.findById(UUID.fromString(id))
                .ifPresent(gameEntity -> {
                    gameEntity.setStatus(GameStatus.FINISHED);
                    gameEntity.setResult(quizGameStateModel.calculateAndGetAnswersResult());
                    gameEntity.setFinishDate(LocalDateTime.now());
                });
    }

    public List<GameEntity> findAllFinishedNewFirst() {
        return gameDao.findAll()
                .stream()
                .filter(gameEntity -> gameEntity.getStatus().equals(GameStatus.FINISHED))
                .sorted(Comparator.comparing(GameEntity::getFinishDate, Comparator.reverseOrder()))
                .toList();
    }
}
