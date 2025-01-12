package org.rsinitsyn.quiz.service;

import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.GameQuestionUserDao;
import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.dao.QuestionGradeDao;
import org.rsinitsyn.quiz.entity.*;
import org.rsinitsyn.quiz.model.AnswerHistory;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.binding.*;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.strategy.update.*;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    public static final String GENERAL_CATEGORY = "Общие";

    private final QuestionDao questionDao;
    private final QuestionCategoryDao questionCategoryDao;
    private final GameQuestionUserDao gameQuestionUserDao;
    private final QuestionGradeDao questionGradeDao;
    private final ResourceService resourceService;
    private final UserService userService;
    private final QuizAppProperties properties;
    private final Map<
            String,
            AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel>> questionUpdateStrategyMap;

    public QuestionService(QuestionDao questionDao,
                           QuestionCategoryDao questionCategoryDao,
                           GameQuestionUserDao gameQuestionUserDao,
                           QuestionGradeDao questionGradeDao,
                           ResourceService resourceService,
                           UserService userService,
                           QuizAppProperties properties,
                           @Qualifier("questionUpdateStrategyMap") Map<String, AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel>> questionUpdateStrategyMap) {
        this.questionDao = questionDao;
        this.questionCategoryDao = questionCategoryDao;
        this.gameQuestionUserDao = gameQuestionUserDao;
        this.questionGradeDao = questionGradeDao;
        this.resourceService = resourceService;
        this.userService = userService;
        this.properties = properties;
        this.questionUpdateStrategyMap = questionUpdateStrategyMap;
    }

    @Cacheable(value = "allCategories")
    public List<QuestionCategoryEntity> findAllCategories() {
        return questionCategoryDao.findAllOrderByName();
    }

    @Cacheable(value = "defaultCategory")
    public QuestionCategoryEntity getOrCreateDefaultCategory() {
        return questionCategoryDao.findByName(GENERAL_CATEGORY)
                .orElseGet(() -> {
                    QuestionCategoryEntity categoryEntity = new QuestionCategoryEntity();
                    categoryEntity.setName(GENERAL_CATEGORY);
                    return questionCategoryDao.save(categoryEntity);
                });
    }

    @CacheEvict(value = "allCategories", allEntries = true)
    public QuestionCategoryEntity saveQuestionCategory(String categoryName) {
        QuestionCategoryEntity entity = new QuestionCategoryEntity();
        entity.setName(categoryName);
        QuestionCategoryEntity saved = questionCategoryDao.save(entity);
        log.info("Category saved, name: {}", saved.getName());
        return saved;
    }

    public Optional<QuestionCategoryEntity> findCategoryByName(String name) {
        return questionCategoryDao.findByName(name);
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
        return questionDao.findAllJoinAnswersAndCategoryNewFirst()
                .stream().filter(entity -> entity.getCreatedBy().equals(username))
                .toList();
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
                                        (gqeRight, gqeWrong) -> gqeRight)));
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
    public void saveOrUpdate(TopQuestionBindingModel model) {
        var strategy = (TopQuestionUpdateStrategy)
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        var question = strategy.prepareEntity(model, model.getId() == null
                ? null
                : findById(UUID.fromString(model.getId())));
        saveEntityAndResources(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(PrecisionQuestionBindingModel model) {
        var strategy = (PreciseQuestionUpdateStrategy)
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        var question = strategy.prepareEntity(model, model.getId() == null
                ? null
                : findById(UUID.fromString(model.getId())));
        saveEntityAndResources(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(OrQuestionBindingModel model) {
        var strategy = (OrQuestionUpdateStrategy)
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        var question = strategy.prepareEntity(model, model.getId() == null
                ? null
                : findById(UUID.fromString(model.getId())));
        saveEntityAndResources(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(PhotoQuestionBindingModel model) {
        var strategy = (PhotoQuestionUpdateStrategy)
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        var question = strategy.prepareEntity(model, model.getId() == null
                ? null
                : findById(UUID.fromString(model.getId())));
        saveEntityAndResources(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(LinkQuestionBindingModel model) {
        var strategy = (LinkQuestionUpdateStrategy)
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        var question = strategy.prepareEntity(model, model.getId() == null
                ? null
                : findById(UUID.fromString(model.getId())));
        saveEntityAndResources(question);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(FourAnswersQuestionBindingModel model) {
        var strategy = (FourAnswersQuestionUpdateStrategy)
                questionUpdateStrategyMap.get(model.getClass().getSimpleName());
        if (model.getId() == null) {
            QuestionEntity saved = questionDao.save(strategy.prepareEntity(model, null));
            resourceService.saveImageFromUrl(saved.getPhotoFilename(), saved.getOriginalPhotoUrl());
            resourceService.saveAudio(saved.getAudioFilename(), model.getAudio());
        } else {
            var persistent = questionDao.findById(UUID.fromString(model.getId())).orElseThrow();
            var updated = strategy.prepareEntity(model, persistent);
            saveEntityAndResources(updated);
            // audio not editable for now
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
