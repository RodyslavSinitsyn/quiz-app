package org.rsinitsyn.quiz.service;

import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.dao.GameQuestionUserDao;
import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.dao.QuestionGradeDao;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionGrade;
import org.rsinitsyn.quiz.entity.QuestionGradeId;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.AnswerHistory;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.binding.AbstractQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.LinkQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.OrQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.PhotoQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.model.binding.TopQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "questionService")
@Service
@RequiredArgsConstructor
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
    public void saveEntityAndImage(QuestionEntity entity) {
        questionDao.save(entity);
        resourceService.saveImageFromUrl(entity.getPhotoFilename(), entity.getOriginalPhotoUrl());
        entity.getAnswers().forEach(answerEntity -> resourceService.saveImageFromUrl(
                answerEntity.getPhotoFilename(),
                answerEntity.getPhotoUrl()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(TopQuestionBindingModel model) {
        AtomicInteger count = new AtomicInteger(0);
        if (model.getId() == null) {
            QuestionEntity question = new QuestionEntity();
            setFieldsForQuestionEntity(question, model, QuestionType.TOP, false);
            setPhotoFieldsForQuestionEntity(question, model.getPhotoLocation());
            model.getTopListText().lines().forEach(
                    answerText -> question.addAnswer(createAnswerEntity(answerText, true, count.getAndIncrement(), null)));
            saveEntityAndImage(question);
        } else {
            QuestionEntity persistent = findByIdLazy(UUID.fromString(model.getId()));
            persistent.setText(model.getText());
            persistent.setAnswerDescriptionText(model.getAnswerDescriptionText());
            setPhotoFieldsForQuestionEntity(persistent, model.getPhotoLocation());
            persistent.getAnswers().clear();
            model.getTopListText().lines().forEach(
                    answerText -> persistent.addAnswer(createAnswerEntity(answerText, true, count.getAndIncrement(), null)));
            saveEntityAndImage(persistent);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(PrecisionQuestionBindingModel model) {
        // NOT UPDATABLE
        if (model.getId() == null) {
            QuestionEntity question = new QuestionEntity();
            question.setValidRange(model.getRange().intValue());
            setFieldsForQuestionEntity(question, model, QuestionType.PRECISION, false);
            setPhotoFieldsForQuestionEntity(question, model.getPhotoLocation());

            AnswerEntity answer = new AnswerEntity();
            answer.setCorrect(true);
            answer.setNumber(0);
            answer.setText(String.valueOf(model.getAnswerText().intValue()));
            question.addAnswer(answer);

            saveEntityAndImage(question);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(OrQuestionBindingModel model) {
        // NOT UPDATABLE
        if (model.getId() == null) {
            QuestionEntity question = new QuestionEntity();
            setFieldsForQuestionEntity(question, model, QuestionType.OR, true);
            setPhotoFieldsForQuestionEntity(question, model.getPhotoLocation());

            AnswerEntity correct = new AnswerEntity();
            correct.setCorrect(true);
            correct.setNumber(0);
            correct.setText(model.getCorrectAnswerText());
            question.addAnswer(correct);

            AnswerEntity option = new AnswerEntity();
            option.setCorrect(false);
            option.setNumber(1);
            option.setText(model.getOptionAnswerText());
            question.addAnswer(option);

            saveEntityAndImage(question);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(PhotoQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity question = new QuestionEntity();
            setFieldsForQuestionEntity(question, model, QuestionType.PHOTO, true);
            setPhotoFieldsForQuestionEntity(question, model.getPhotoLocation());

            question.addAnswer(createAnswerEntity("A", true, 0, model.getCorrectOption()));
            question.addAnswer(createAnswerEntity("B", false, 1, model.getOptionTwo()));
            question.addAnswer(createAnswerEntity("C", false, 2, model.getOptionThree()));
            question.addAnswer(createAnswerEntity("D", false, 3, model.getOptionFour()));

            saveEntityAndImage(question);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(LinkQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity question = new QuestionEntity();
            setFieldsForQuestionEntity(question, model, QuestionType.LINK, true);
            setPhotoFieldsForQuestionEntity(question, model.getPhotoLocation());

            AtomicInteger counter = new AtomicInteger(0);
            model.getLeftAnswers().lines().forEach(
                    line -> question.addAnswer(createAnswerEntity(line, true, counter.getAndIncrement(), null)));

            counter.set(0);
            model.getRightAnswers().lines().forEach(
                    line -> question.addAnswer(createAnswerEntity(line, false, counter.getAndIncrement(), null)));

            saveEntityAndImage(question);
        }
    }

    private void setFieldsForQuestionEntity(QuestionEntity question,
                                            AbstractQuestionBindingModel model,
                                            QuestionType type,
                                            boolean optionsOnly) {
        question.setText(model.getText());
        question.setCreatedBy(SessionWrapper.getLoggedUser());
        question.setCreationDate(LocalDateTime.now());
        question.setAnswerDescriptionText(model.getAnswerDescriptionText());
        question.setCategory(getOrCreateDefaultCategory());
        question.setType(type);
        question.setOptionsOnly(optionsOnly);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(FourAnswersQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity saved = questionDao.save(toQuestionEntity(model, null));
            resourceService.saveImageFromUrl(saved.getPhotoFilename(), saved.getOriginalPhotoUrl());
            resourceService.saveAudio(saved.getAudioFilename(), model.getAudio());
            log.info("Question saved: {}", saved);
        } else {
            var persistent = questionDao.findById(UUID.fromString(model.getId())).orElseThrow();
            String oldPhotoFilename = persistent.getPhotoFilename();

            QuestionEntity updated = questionDao.save(toQuestionEntity(model, persistent));
            if (oldPhotoFilename == null) {
                resourceService.saveImageFromUrl(updated.getPhotoFilename(), updated.getOriginalPhotoUrl());
            } else if (!oldPhotoFilename.equals(updated.getPhotoFilename())) {
                log.debug("Replace image file, questionId: {}", updated.getId());
                resourceService.saveImageFromUrl(updated.getPhotoFilename(), updated.getOriginalPhotoUrl());
                resourceService.deleteImageFile(oldPhotoFilename);
            }
            log.info("Question updated: {}", updated);
        }
    }

    public QuestionEntity toQuestionEntity(FourAnswersQuestionBindingModel model,
                                           QuestionEntity persistEntity) {
        boolean update = persistEntity != null;
        QuestionEntity newEntity = new QuestionEntity();

        newEntity.setText(model.getText());
        newEntity.setAnswerDescriptionText(model.getAnswerDescriptionText());
        if (update) {
            newEntity.setId(UUID.fromString(model.getId()));
            newEntity.setCreationDate(persistEntity.getCreationDate());
            newEntity.setCreatedBy(model.getAuthor());
            newEntity.setOptionsOnly(persistEntity.isOptionsOnly());
            newEntity.setAudioFilename(persistEntity.getAudioFilename());
            newEntity.setGrades(persistEntity.getGrades());
            newEntity.setAnswers(persistEntity.getAnswers());
            updateAnswerEntities(newEntity, model.getAnswers());
        } else {
            newEntity.setId(UUID.randomUUID());
            newEntity.setCreationDate(LocalDateTime.now());
            newEntity.setCreatedBy(SessionWrapper.getLoggedUser());
            newEntity.setOptionsOnly(false);

            if (model.getAudio() != null) {
                newEntity.setAudioFilename(properties.getFilesFolder() + QuizUtils.generateFilenameWithExt(".mp3"));
            }
            createAnswerEntities(newEntity, model.getAnswers());
        }

        setPhotoFieldsForQuestionEntity(newEntity, model.getPhotoLocation());

        if (model.hasMultiCorrectOptions()) {
            newEntity.setType(QuestionType.MULTI);
        } else {
            newEntity.setType(QuestionType.TEXT);
        }

        questionCategoryDao.findByName(model.getCategory())
                .ifPresentOrElse(
                        newEntity::setCategory,
                        () -> {
                            QuestionCategoryEntity defaultCategory = getOrCreateDefaultCategory();
                            newEntity.setCategory(defaultCategory);
                        });
        return newEntity;
    }

    private void setPhotoFieldsForQuestionEntity(QuestionEntity question, String photoUrl) {
        if (StringUtils.isNotBlank(photoUrl)) {
            question.setOriginalPhotoUrl(photoUrl);
            question.setPhotoFilename(
                    properties.getFilesFolder() + QuizUtils.generateFilename(photoUrl));
        } else {
            question.setPhotoFilename(null);
            question.setOriginalPhotoUrl(null);
        }
    }

    private void updateAnswerEntities(
            QuestionEntity entity,
            List<FourAnswersQuestionBindingModel.AnswerBindingModel> answerModels) {
        Function<UUID, FourAnswersQuestionBindingModel.AnswerBindingModel> getById = id ->
                answerModels
                        .stream()
                        .filter(am -> am.getId().equals(id))
                        .findFirst().orElseThrow();
        entity.getAnswers().forEach(answerEntity -> {
            FourAnswersQuestionBindingModel.AnswerBindingModel answerMOdel = getById.apply(answerEntity.getId());
            answerEntity.setText(answerMOdel.getText());
            answerEntity.setNumber(answerMOdel.getIndex());
            answerEntity.setCorrect(answerMOdel.isCorrect());
        });
    }

    private void createAnswerEntities(
            QuestionEntity entity,
            List<FourAnswersQuestionBindingModel.AnswerBindingModel> answerModels) {
        AtomicInteger number = new AtomicInteger(0);
        answerModels.forEach(answerBindingModel -> {
            entity.addAnswer(createAnswerEntity(answerBindingModel.getText(),
                    answerBindingModel.isCorrect(),
                    number.getAndIncrement(),
                    null));
        });
    }

    private AnswerEntity createAnswerEntity(String text,
                                            boolean correct,
                                            int number,
                                            String photoUrl) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setText(text);
        answerEntity.setCorrect(correct);
        answerEntity.setNumber(number);
        if (photoUrl != null) {
            answerEntity.setPhotoFilename(properties.getFilesFolder() + QuizUtils.generateFilename(photoUrl));
            answerEntity.setPhotoUrl(photoUrl);
        } else {
            answerEntity.setPhotoFilename(null);
            answerEntity.setPhotoUrl(null);
        }
        return answerEntity;
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
