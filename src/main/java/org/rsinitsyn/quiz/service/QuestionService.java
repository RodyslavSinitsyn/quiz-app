package org.rsinitsyn.quiz.service;

import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameQuestionUserDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.dao.QuestionGradeDao;
import org.rsinitsyn.quiz.entity.*;
import org.rsinitsyn.quiz.model.AnswerHistory;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.binding.AbstractQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.service.strategy.update.AbstractQuestionUpdateStrategy;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Observed(name = "questionService")
@Service
@Slf4j
public class QuestionService {

    private final QuestionDao questionDao;
    private final GameQuestionUserDao gameQuestionUserDao;
    private final QuestionGradeDao questionGradeDao;
    private final ResourceService resourceService;
    private final UserService userService;
    private Map<
            String,
            AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel>> questionUpdateStrategyMap;

    public QuestionService(QuestionDao questionDao,
                           GameQuestionUserDao gameQuestionUserDao,
                           QuestionGradeDao questionGradeDao,
                           ResourceService resourceService,
                           UserService userService,
                           @Qualifier("questionUpdateStrategyMap") Map<String, AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel>> questionUpdateStrategyMap) {
        this.questionDao = questionDao;
        this.gameQuestionUserDao = gameQuestionUserDao;
        this.questionGradeDao = questionGradeDao;
        this.resourceService = resourceService;
        this.userService = userService;
        this.questionUpdateStrategyMap = questionUpdateStrategyMap;
    }

    public QuestionEntity findByIdLazy(UUID id) {
        return questionDao.getReferenceById(id);
    }

    public QuestionEntity findById(UUID id) {
        return questionDao.findById(id).orElseThrow();
    }

    @Transactional
    public QuestionEntity findByIdJoinAnswers(UUID id) {
        return questionDao.findByIdJoinAnswers(id).orElseThrow();
    }

    public List<QuestionEntity> findAllCreatedByCurrentUser() {
        return findAllCreatedByUser(SessionWrapper.getLoggedUser());
    }

    public List<QuestionEntity> findAllCreatedByUser(String username) {
        if (username.equals("admin")) {
            return questionDao.findAllJoinAnswersAndCategoryNewFirst();
        }
        return questionDao.findAllJoinAnswersAndCategoryNewFirst(username);
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<QuestionModel> findAllByCurrentUserAsModel() {

        List<QuestionEntity> questionsCreatedByCurrentUser = findAllCreatedByCurrentUser();
        List<GameQuestionUserEntity> questionsFromAllGames =
                gameQuestionUserDao.findAllByQuestionIdIn(
                        questionsCreatedByCurrentUser.stream()
                                .map(QuestionEntity::getId)
                                .collect(Collectors.toList())
                );

        Function<QuestionEntity, List<GameQuestionUserEntity>> getQuestionHistory =
                qe -> questionsFromAllGames.stream()
                        .filter(gqe -> gqe.getGame().getStatus().equals(GameStatus.FINISHED))
                        .filter(gqe -> !gqe.getUser().getUsername().equals(SessionWrapper.getLoggedUser()))
                        .filter(gqe -> gqe.getQuestion().getId().equals(qe.getId()))
                        .toList();

        return questionsCreatedByCurrentUser
                .stream()
                .map(question -> {
                    Map<String, AnswerHistory> answerHistoryMap = new HashMap<>();

                    List<GameQuestionUserEntity> questionHistory = getQuestionHistory.apply(question);
                    if (!questionHistory.isEmpty()) {
                        answerHistoryMap.putAll(questionHistory.stream()
                                .sorted(Comparator.comparing(GameQuestionUserEntity::getAnswered, Comparator.nullsLast(Comparator.reverseOrder())))
                                .collect(Collectors.toMap(
                                        gqe -> gqe.getUser().getUsername(),
                                        gqe -> AnswerHistory.ofAnswerResult(gqe.getAnswered()),
                                        (correctAnswer, wrongAnswer) -> correctAnswer)));
                    }

                    QuestionModel questionModel = toQuizQuestionModel(question);
                    questionModel.setPlayersAnswersHistory(answerHistoryMap);
                    return questionModel;
                })
                .toList();
    }

    public QuestionModel toQuizQuestionModel(QuestionEntity question) {
        return QuestionModel.builder()
                .id(question.getId())
                .text(question.getText())
                .type(question.getType())
                .categoryName(question.getCategory().getName())
                .answers(toQuizAnswerModel(question.getAnswers()))
                .photoFilename(question.getPhotoFilename())
                .audioFilename(question.getAudioFilename())
                .optionsOnly(question.isOptionsOnly())
                .validRange(question.getValidRange())
                .answerDescription(question.getAnswerDescriptionText())
                .build();
    }

    private Set<QuestionModel.AnswerModel> toQuizAnswerModel(Set<AnswerEntity> answerEntitySet) {
        return answerEntitySet.stream()
                .map(answerEntity -> new QuestionModel.AnswerModel(
                        answerEntity.getText(),
                        answerEntity.isCorrect(),
                        answerEntity.getNumber(),
                        answerEntity.getPhotoFilename()))
                .collect(Collectors.toSet());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public <T extends AbstractQuestionBindingModel> void saveOrUpdate(T model) {
        AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel> abstractStrategy =
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        @SuppressWarnings("unchecked")
        AbstractQuestionUpdateStrategy<T> specificStrategy = (AbstractQuestionUpdateStrategy<T>) abstractStrategy;
        var question = specificStrategy.prepareEntity(model, model.getId() == null
                ? null
                : findById(UUID.fromString(model.getId())));
        saveEntityAndResources(question);
        // TODO: Temp solution for saving audio only for one question type
        if (model instanceof FourAnswersQuestionBindingModel fourAnswersModel) {
            if (fourAnswersModel.getId() == null) {
                resourceService.saveAudio(question.getAudioFilename(), fourAnswersModel.getAudio());
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEntityAndResources(QuestionEntity entity) {
        questionDao.save(entity);
        if (entity.isShouldSaveImage()) {
            resourceService.saveImageFromUrl(entity.getPhotoFilename(), entity.getOriginalPhotoUrl());
        }
        entity.getResourcesToDelete().forEach(resourceService::deleteImageFile);
        entity.getAnswers().forEach(answerEntity -> resourceService.saveImageFromUrl(
                answerEntity.getPhotoFilename(),
                answerEntity.getPhotoUrl()));
        log.info("Question saved/updated: {}", entity);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateCategory(Set<QuestionEntity> questions, QuestionCategoryEntity category) {
        questionDao.findAllByIdIn(questions.stream().map(QuestionEntity::getId).toList())
                .forEach(entity -> entity.setCategory(category));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateOptionsOnlyProperty(Set<QuestionEntity> questions) {
        questionDao.findAllByIdIn(questions.stream().map(QuestionEntity::getId).toList())
                .forEach(entity -> entity.setOptionsOnly(!entity.isOptionsOnly()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateQuestionGrade(UUID questionId, String username, int grade) {
        UserEntity user = userService.findByUsername(username);
        Optional<QuestionGrade> optEntity = questionGradeDao.findById(new QuestionGradeId(
                questionId,
                user.getId()
        ));
        if (optEntity.isPresent()) {
            QuestionGrade updEntity = optEntity.get();
            updEntity.setGrade(grade);
            log.info("Updated question grade, id: {}, grade: {}",
                    questionId + "-" + user.getId(),
                    grade);
        } else {
            var newEntity = new QuestionGrade();
            newEntity.setQuestionId(questionId);
            newEntity.setUserId(user.getId());
            newEntity.setGrade(grade);
            questionGradeDao.save(newEntity);
            log.info("Created question grade, id: {}, grade: {}",
                    questionId + "-" + user.getId(),
                    grade);
        }
    }

    public void deleteById(String id) {
        questionDao.findByIdJoinAnswers(UUID.fromString(id)).ifPresent(this::delete);
    }

    /*
        Non batch operation.
     */
    public void deleteAll(Collection<QuestionEntity> questions) {
        questions.forEach(this::delete);
    }

    @Transactional
    public void delete(QuestionEntity entity) {
        if (questionDao.deleteQuestionEntityById(entity.getId()) == 1) {
            resourceService.deleteImageFile(entity.getPhotoFilename());
            resourceService.deleteAudioFile(entity.getAudioFilename());
            entity.getAnswers().forEach(answerEntity -> resourceService.deleteImageFile(answerEntity.getPhotoFilename()));
        }
    }
}
