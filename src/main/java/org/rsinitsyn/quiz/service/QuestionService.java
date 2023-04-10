package org.rsinitsyn.quiz.service;

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
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.AnswerHistory;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.PrecisionQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    public static final String GENERAL_CATEGORY = "Общие";

    private final QuestionDao questionDao;
    private final QuestionCategoryDao questionCategoryDao;
    private final GameQuestionUserDao gameQuestionUserDao;
    private final ResourceService resourceService;
    private final QuizAppProperties properties;

    public List<QuestionCategoryEntity> findAllCategories() {
        return questionCategoryDao.findAll();
    }

    public QuestionCategoryEntity getOrCreateDefaultCategory() {
        return questionCategoryDao.findByName(GENERAL_CATEGORY)
                .orElseGet(() -> {
                    QuestionCategoryEntity categoryEntity = new QuestionCategoryEntity();
                    categoryEntity.setName(GENERAL_CATEGORY);
                    return questionCategoryDao.save(categoryEntity);
                });

    }

    public void saveQuestionCategory(QuestionCategoryBindingModel model) {
        QuestionCategoryEntity entity = new QuestionCategoryEntity();
        entity.setName(model.getCategoryName());
        QuestionCategoryEntity saved = questionCategoryDao.save(entity);
        log.info("Category saved, name: {}", saved.getName());
    }

    public QuestionEntity findByIdLazy(UUID id) {
        return questionDao.getReferenceById(id);
    }

    public List<QuestionEntity> findAll() {
        return questionDao.findAll();
    }

    public List<QuestionEntity> findAllNewFirst() {
        return questionDao.findAll()
                .stream()
                .sorted(Comparator.comparing(QuestionEntity::getCreationDate, Comparator.reverseOrder()))
                .toList();
    }

    public List<QuestionEntity> findAllByCurrentUser() {
        String loggedUser = QuizUtils.getLoggedUser();
        if (loggedUser.equals("admin")) {
            return findAllNewFirst();
        }
        return findAllNewFirst()
                .stream().filter(entity -> entity.getCreatedBy().equals(loggedUser))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public List<QuizQuestionModel> findAllByCurrentUserAsModel() {

        List<QuestionEntity> questionsCreatedByCurrentUser = findAllByCurrentUser();
        List<GameQuestionUserEntity> questionsFromAllGames =
                gameQuestionUserDao.findAllByQuestionIdIn(
                        questionsCreatedByCurrentUser.stream()
                                .map(QuestionEntity::getId)
                                .collect(Collectors.toList())
                );

        Function<QuestionEntity, List<GameQuestionUserEntity>> getQuestionHistory =
                qe -> questionsFromAllGames.stream()
                        .filter(gqe -> gqe.getGame().getStatus().equals(GameStatus.FINISHED)
                                // Todo For cleverest set user or refactor approach
                                && gqe.getGame().getType().equals(GameType.QUIZ))
                        .filter(gqe -> !gqe.getGame().getPlayerNames().contains(QuizUtils.getLoggedUser()))
                        .filter(gqe -> gqe.getQuestion().getId().equals(qe.getId()))
                        .toList();

        return findAllByCurrentUser()
                .stream()
                .map(question -> {
                    Map<String, AnswerHistory> answerHistoryMap = new HashMap<>();

                    List<GameQuestionUserEntity> questionHistory = getQuestionHistory.apply(question);
                    if (!questionHistory.isEmpty()) {
                        answerHistoryMap = questionHistory.stream()
                                .sorted(Comparator.comparing(GameQuestionUserEntity::getAnswered, Comparator.reverseOrder()))
                                .collect(Collectors.toMap(
                                        // TODO Refactor
                                        gqe -> gqe.getGame().getPlayerNames()
                                                .stream()
                                                .filter(s -> s.equals(QuizUtils.getLoggedUser())).findFirst().orElseThrow(),
                                        gqe -> AnswerHistory.ofAnswerResult(gqe.getAnswered()),
                                        (gqeRight, gqeWrong) -> gqeRight));
                    }

                    QuizQuestionModel questionModel = toQuizQuestionModel(question);
                    questionModel.setPlayersAnswersHistory(answerHistoryMap);
                    return questionModel;
                })
                .toList();
    }

    public QuizQuestionModel toQuizQuestionModel(QuestionEntity question) {
        return QuizQuestionModel.builder()
                .id(question.getId())
                .text(question.getText())
                .type(question.getType())
                .categoryName(question.getCategory().getName())
                .answers(toQuizAnswerModel(question.getAnswers()))
                .photoFilename(question.getPhotoFilename())
                .audioFilename(question.getAudioFilename())
                .optionsOnly(question.isOptionsOnly())
                .validRange(question.getValidRange())
                .build();
    }

    private Set<QuizQuestionModel.QuizAnswerModel> toQuizAnswerModel(Set<AnswerEntity> answerEntitySet) {
        return answerEntitySet.stream()
                .map(answerEntity -> new QuizQuestionModel.QuizAnswerModel(
                        answerEntity.getText(),
                        answerEntity.isCorrect()))
                .collect(Collectors.toSet());
    }

    public void saveAll(Collection<QuestionEntity> entities) {
        questionDao.saveAll(entities);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveEntityAndImage(QuestionEntity entity) {
        questionDao.save(entity);
        resourceService.saveImage(entity.getPhotoFilename(), entity.getOriginalPhotoUrl());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteById(String id) {
        Optional<QuestionEntity> toDelete = questionDao.findById(UUID.fromString(id));
        toDelete.ifPresent(entity -> {
            questionDao.delete(entity);
            resourceService.deleteImageFile(entity.getPhotoFilename());
        });
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(PrecisionQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity question = new QuestionEntity();
            question.setText(model.getText());
            question.setCreatedBy(QuizUtils.getLoggedUser());
            question.setCreationDate(LocalDateTime.now());
            question.setType(QuestionType.PRECISION);
            question.setOptionsOnly(false);
            question.setCategory(getOrCreateDefaultCategory());
            question.setValidRange(model.getRange().intValue());

            AnswerEntity answer = new AnswerEntity();
            answer.setCorrect(true);
            answer.setNumber(0);
            answer.setText(String.valueOf(model.getAnswerText().intValue()));
            question.addAnswer(answer);

            questionDao.save(question);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(FourAnswersQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity saved = questionDao.save(toQuestionEntity(model, Optional.empty()));
            resourceService.saveImage(saved.getPhotoFilename(), saved.getOriginalPhotoUrl());
            resourceService.saveAudio(saved.getAudioFilename(), model.getAudio());
            log.info("Question saved: {}", saved);
        } else {
            QuestionEntity updated = questionDao.save(toQuestionEntity(model, questionDao.findById(UUID.fromString(model.getId()))));
            resourceService.saveImage(updated.getPhotoFilename(), updated.getOriginalPhotoUrl());
            log.info("Question updated: {}", updated);
        }
    }

    public QuestionEntity toQuestionEntity(FourAnswersQuestionBindingModel model, Optional<QuestionEntity> optEntity) {
        boolean update = optEntity.isPresent();

        QuestionEntity entity = new QuestionEntity();

        if (update) {
            entity.setId(UUID.fromString(model.getId()));
            entity.setCreationDate(optEntity.get().getCreationDate());
            entity.setCreatedBy(model.getAuthor());
            entity.setOptionsOnly(optEntity.get().isOptionsOnly());

            entity.setAudioFilename(optEntity.get().getAudioFilename());
        } else {
            entity.setId(UUID.randomUUID());
            entity.setCreationDate(LocalDateTime.now());
            entity.setCreatedBy(QuizUtils.getLoggedUser());
            entity.setOptionsOnly(true);

            if (model.getAudio() != null) {
                entity.setAudioFilename(properties.getAudioFolder() + QuizUtils.generateFilenameWithExt(".mp3"));
            }
        }
        entity.setText(model.getText());

        AtomicInteger number = new AtomicInteger(0);
        model.getAnswers().forEach(answerBindingModel -> {
            entity.addAnswer(createAnswerEntity(answerBindingModel.getText(),
                    answerBindingModel.isCorrect(),
                    number.getAndIncrement()));
        });

        if (StringUtils.isNotBlank(model.getPhotoLocation())) {
            entity.setOriginalPhotoUrl(model.getPhotoLocation());
            entity.setPhotoFilename(QuizUtils.generateFilename(model.getPhotoLocation()));
        } else {
            entity.setPhotoFilename(null);
            entity.setOriginalPhotoUrl(null);
        }

        if (model.hasMultiCorrectOptions()) {
            entity.setType(QuestionType.MULTI);
        } else {
            entity.setType(QuestionType.TEXT);
        }

        questionCategoryDao.findByName(model.getCategory())
                .ifPresentOrElse(
                        entity::setCategory,
                        () -> {
                            QuestionCategoryEntity defaultCategory = getOrCreateDefaultCategory();
                            entity.setCategory(defaultCategory);
                        });
        return entity;
    }

    private AnswerEntity createAnswerEntity(String text, boolean correct, int number) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setText(text);
        answerEntity.setCorrect(correct);
        answerEntity.setNumber(number);
        return answerEntity;
    }

    public void deleteAll(Collection<QuestionEntity> questions) {
        questionDao.deleteAll(questions);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateCategory(Set<QuestionEntity> questions, QuestionCategoryEntity category) {
        List<QuestionEntity> persistentQuestions = questionDao.findAllByIdIn(questions.stream().map(QuestionEntity::getId).toList());
        persistentQuestions.forEach(entity -> entity.setCategory(category));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void updateOptionsOnlyProperty(Set<QuestionEntity> questions) {
        questionDao.findAllByIdIn(questions.stream().map(QuestionEntity::getId).toList())
                .forEach(entity -> entity.setOptionsOnly(!entity.isOptionsOnly()));
    }
}
