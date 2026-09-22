package org.rsinitsyn.quiz.service;

import io.micrometer.observation.annotation.Observed;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.rsinitsyn.quiz.dao.GameDao;
import org.rsinitsyn.quiz.dao.GameQuestionUserDao;
import org.rsinitsyn.quiz.entity.*;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.rsinitsyn.quiz.view.GameDetailsView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.rsinitsyn.quiz.entity.AnswerStatus.UNKNOWN;
import static org.rsinitsyn.quiz.entity.GameStatus.FINISHED;
import static org.rsinitsyn.quiz.entity.GameStatus.NOT_STARTED;

@Observed(name = "gameService")
@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {
    private final GameDao gameDao;
    private final GameQuestionUserDao gameQuestionUserDao;
    private final QuestionService questionService;
    private final GameQuestionUserAnswerService gameQuestionUserAnswerService;
    private final UserService userService;
    private final EntityManager entityManager;

    public boolean exist(String id) {
        return gameDao.existsById(UUID.fromString(id));
    }

    public GameStatus getStatus(String gameId) {
        return gameDao.findById(UUID.fromString(gameId)).map(GameEntity::getStatus).orElse(null);
    }

    @Transactional(readOnly = true)
    public GameEntity findById(String id) {
        return gameDao.findByIdJoinQuestionsOld(UUID.fromString(id)).stream()
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

    @Transactional(readOnly = true)
    public GameEntity findByIdNew(UUID id) {
        // join questions
        final var gameEntityWithQuestions = gameDao.findByIdJoinQuestions(id);
        // join participants
        gameDao.findByIdJoinParticipants(id);
        // backward compatibility
        gameDao.findByIdJoinQuestionsOld(id);
        return gameEntityWithQuestions.stream()
                .peek(gq -> {
                    gq.getQuestions().stream()
                            .map(GameQuestionEntity::getQuestion)
                            .forEach(q -> {
                                Hibernate.initialize(q.getAnswers());
                                Hibernate.initialize(q.getGrades());
                                Hibernate.initialize(q.getHints());
                            });
                })
                .findFirst()
                .orElse(null);
    }

    @Deprecated
    public List<GameEntity> findAllNewFirstOld() {
        return gameDao.findAllJoinGamesQuestionsNewFirstOld();
    }

    @Transactional(readOnly = true)
    public List<GameDetailsView> getAllGameDetails() {
        // dual read
        final var gameDetails = gameDao.findAllGameDetails();
        final var oldGameDetails = findAllNewFirstOld().stream()
                .map(g -> new GameDetailsView() {
                    @Override
                    public UUID getId() {
                        return g.getId();
                    }

                    @Override
                    public GameStatus getStatus() {
                        return g.getStatus();
                    }

                    @Override
                    public String getCreatedBy() {
                        return g.getCreatedBy();
                    }

                    @Override
                    public GameType getType() {
                        return g.getType();
                    }

                    @Override
                    public String getName() {
                        return g.getName();
                    }

                    @Override
                    public String[] getPlayerNames() {
                        return g.getPlayerNames().toArray(String[]::new);
                    }

                    @Override
                    public int getAnsweredQuestions() {
                        return (int) g.getGameQuestions().stream().filter(GameQuestionUserEntity::getAnswered).count();
                    }

                    @Override
                    public int getTotalQuestions() {
                        return (int) g.getGameQuestions().size();
                    }

                    @Override
                    public int getCorrectAnswers() {
                        return (int) g.getGameQuestions().stream().filter(GameQuestionUserEntity::getAnswered).count();
                    }

                    @Override
                    public LocalDateTime getCreationDate() {
                        return g.getCreationDate();
                    }

                    @Override
                    public LocalDateTime getFinishDate() {
                        return g.getFinishDate();
                    }

                    @Override
                    public boolean oldSource() {
                        return true;
                    }
                })
                .toList();
        return Stream.concat(gameDetails.stream(), oldGameDetails.stream())
                .sorted(Comparator.comparing(GameDetailsView::getCreatedBy).reversed())
                .toList();
    }

    public boolean exists(UUID id) {
        return gameDao.existsById(id);
    }

    @Transactional
    public void submitAnswersBatch(String gameId,
                                   QuestionModel question,
                                   List<UserStateSnapshot> userAnswers) {
        userAnswers.forEach(answerSnapshot -> {
            submitAnswers(
                    gameId,
                    answerSnapshot.username(),
                    question,
                    Collections.singletonList(answerSnapshot.answerText()),
                    answerSnapshot::answerStatus);
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void submitAnswers(String gameId,
                              String playerName,
                              QuestionModel questionModel,
                              List<String> answersList,
                              Supplier<AnswerStatus> answerStatusSupplier) {
        UserEntity user = userService.findByUsername(playerName);
        var primaryKey = new GameQuestionUserId(
                UUID.fromString(gameId),
                questionModel.getId(),
                user.getId());
        Optional<GameQuestionUserEntity> optEntity = gameQuestionUserDao.findById(primaryKey);
        if (optEntity.isPresent()) {
            GameQuestionUserEntity persistent = optEntity.get();
            persistent.setAnswerStatus(answerStatusSupplier.get());
            persistent.setAnswerText(String.join(",", answersList));

            gameQuestionUserDao.save(persistent);
        } else {
            GameQuestionUserEntity newEntity = new GameQuestionUserEntity();
            newEntity.setId(primaryKey);
            newEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
            newEntity.setUser(user);
            newEntity.setGame(findById(gameId));
            newEntity.setAnswerStatus(answerStatusSupplier.get());
            newEntity.setAnswerText(String.join(",", answersList));
            newEntity.setOrderNumber(gameQuestionUserDao.getMaxOrderNumber(primaryKey.getGameId()) + 1);
            gameQuestionUserDao.save(newEntity);
        }
    }

    public boolean createIfNotExists(UUID id,
                                     String name,
                                     GameType gameType,
                                     Optional<GameConfiguration> configuration) {
        if (gameDao.existsById(id)) {
            log.info("Game already exists, id: {}", id);
            return false;
        }
        GameEntity entity = new GameEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setStatus(NOT_STARTED);
        entity.setType(gameType);
        entity.setCreatedBy(SessionWrapper.getLoggedUser());
        entity.setCreationDate(LocalDateTime.now());
        configuration.ifPresent(entity::setConfiguration);

        GameEntity saved = gameDao.save(entity);
        log.info("Game created, id: {}", saved.getId());
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateStatus(String id, GameStatus status) {
        entityManager.createQuery("update GameEntity set status = :status where id = :id")
                .setParameter("status", status)
                .setParameter("id", UUID.fromString(id))
                .executeUpdate();
        log.info("Updated game, id: {}", id);
    }

    @Deprecated
    @Transactional(propagation = Propagation.REQUIRED)
    public void linkQuestionsWithGame(String id, QuizGameState stateModel) {
        GameEntity gameEntity = findById(id);
        log.info("Updating game, id: {}", id);
        UserEntity user = userService.findByUsername(stateModel.getPlayerName());
        AtomicInteger questionOrder = new AtomicInteger(0);
        List<GameQuestionUserEntity> gameQuestionEntitiesToSave = stateModel.getQuestions().stream().map(questionModel -> {
            GameQuestionUserEntity gameQuestionUserEntity = new GameQuestionUserEntity();
            gameQuestionUserEntity.setId(new GameQuestionUserId(
                    UUID.fromString(id),
                    questionModel.getId(),
                    user.getId()));
            gameQuestionUserEntity.setAnswerStatus(UNKNOWN);
            gameQuestionUserEntity.setOrderNumber(questionOrder.getAndIncrement());
            gameQuestionUserEntity.setGame(gameEntity);
            gameQuestionUserEntity.setUser(user);
            gameQuestionUserEntity.setQuestion(questionService.findByIdLazy(questionModel.getId()));
            return gameQuestionUserEntity;
        }).toList();

        List<GameQuestionUserEntity> savedGameQuestions = gameQuestionUserDao.saveAll(gameQuestionEntitiesToSave);
        log.info("Saved game questions, size: {}", savedGameQuestions.size());
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
                gameQuestionUserEntity.setAnswerStatus(UNKNOWN);

                entities.add(gameQuestionUserEntity);
            });
        });

        gameQuestionUserDao.saveAll(entities);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void finishGame(String id) {
        updateStatus(id, FINISHED);
    }

    @Transactional
    public void deleteAllBatch(Collection<GameEntity>... gameEntities) {
        Arrays.stream(gameEntities).forEach(gameDao::deleteAll);
        log.debug("Games deleted, size: {}", Arrays.stream(gameEntities).mapToLong(Collection::size).sum());
    }
}
