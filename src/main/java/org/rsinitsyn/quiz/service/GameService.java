package org.rsinitsyn.quiz.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameQuestionDao;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionPrimaryKey;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
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
    private final GameQuestionDao gameQuestionDao;
    private final QuestionService questionService;

    public GameEntity findById(String id) {
        return gameDao.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Game not found. Id: " + id));
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void submitAnswer(String gameId, String questionId, QuizQuestionModel.QuizAnswerModel answerModel) {
        var primaryKey = new GameQuestionPrimaryKey(
                UUID.fromString(gameId), UUID.fromString(questionId));
        GameQuestionEntity gameQuestionEntity = gameQuestionDao.findById(primaryKey).orElseThrow();
        gameQuestionEntity.setAnswered(answerModel.isCorrect());
    }

    public boolean createIfNotExists(String id) {
        Optional<GameEntity> gameEntityOptional = gameDao.findById(UUID.fromString(id));
        if (gameEntityOptional.isPresent()) {
            log.info("Game already exists, id: {}", id);
            return false;
        }
        GameEntity entity = new GameEntity();
        entity.setId(UUID.fromString(id));
        entity.setStatus(GameStatus.NOT_STARTED);
        entity.setType(GameType.QUIZ); // TODO Obrain from UI
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
    public void updateBeforeStart(String id, QuizGameStateModel stateModel) {
        GameEntity gameEntity = findById(id);
        setNewFields(gameEntity, stateModel.getGameName(), stateModel.getPlayerName(), GameStatus.STARTED, stateModel.getQuestions().size(), stateModel.calculateAndGetAnswersResult());
        log.info("Updating game, id: {}", id);

        AtomicInteger questionOrder = new AtomicInteger(0);
        List<GameQuestionEntity> gameQuestionEntitiesToSave = stateModel.getQuestions().stream().map(questionModel -> {
            GameQuestionEntity gameQuestionEntity = new GameQuestionEntity();
            gameQuestionEntity.setId(new GameQuestionPrimaryKey(UUID.fromString(id), questionModel.getId()));
            gameQuestionEntity.setAnswered(null);
            gameQuestionEntity.setOrderNumber(questionOrder.getAndIncrement());
            gameQuestionEntity.setGame(gameEntity);
            gameQuestionEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
            return gameQuestionEntity;
        }).toList();

        List<GameQuestionEntity> savedGameQuestions = gameQuestionDao.saveAll(gameQuestionEntitiesToSave);
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
    public void finish(String id, QuizGameStateModel quizGameStateModel) {
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
