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
import org.rsinitsyn.quiz.dao.GameQuestionDao;
import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.AnswerHistory;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
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
    private final GameQuestionDao gameQuestionDao;


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
    public List<QuizQuestionModel> findAllByCurrentUserAsModel(List<UserEntity> players) {

        List<QuestionEntity> questionsCreatedByCurrentUser = findAllByCurrentUser();
        List<GameQuestionEntity> questionsFromAllGames =
                gameQuestionDao.findAllByQuestionIdIn(
                        questionsCreatedByCurrentUser.stream()
                                .map(QuestionEntity::getId)
                                .collect(Collectors.toList())
                );

        Function<QuestionEntity, List<GameQuestionEntity>> getQuestionHistory =
                qe -> questionsFromAllGames.stream()
                        .filter(gqe -> gqe.getGame().getStatus().equals(GameStatus.FINISHED))
                        .filter(gqe -> !gqe.getGame().getPlayerName().equals(QuizUtils.getLoggedUser()))
                        .filter(gqe -> gqe.getQuestion().getId().equals(qe.getId()))
                        .toList();

        return findAllByCurrentUser()
                .stream()
                .map(question -> {
                    Map<String, AnswerHistory> answerHistoryMap = new HashMap<>();

                    List<GameQuestionEntity> questionHistory = getQuestionHistory.apply(question);

                    if (!questionHistory.isEmpty()) {
                        answerHistoryMap = questionHistory.stream()
                                .sorted(Comparator.comparing(GameQuestionEntity::getAnswered, Comparator.reverseOrder()))
                                .collect(Collectors.toMap(
                                        gqe -> gqe.getGame().getPlayerName(),
                                        gqe -> AnswerHistory.ofAnswerResult(gqe.getAnswered()),
                                        (gqeRight, gqeWrong) -> gqeRight));
                    }

                    return QuizQuestionModel.builder()
                            .id(question.getId())
                            .text(question.getText())
                            .type(question.getType())
                            .categoryName(question.getCategory().getName())
                            .answers(toQuizAnswerModel(question.getAnswers()))
                            .photoFilename(question.getPhotoFilename())
                            .playersAnswersHistory(answerHistoryMap)
                            .optionsEnabled(true)
                            .build();
                })
                .toList();
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
        QuizUtils.saveImage(entity.getPhotoFilename(), entity.getOriginalPhotoUrl());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteById(String id) {
        Optional<QuestionEntity> toDelete = questionDao.findById(UUID.fromString(id));
        toDelete.ifPresent(entity -> {
            questionDao.delete(entity);
            QuizUtils.deleteImageFile(entity.getPhotoFilename());
        });
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(FourAnswersQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity saved = questionDao.save(toQuestionEntity(model, Optional.empty()));
            QuizUtils.saveImage(saved.getPhotoFilename(), saved.getOriginalPhotoUrl());
            log.info("Question saved: {}", saved);
        } else {
            QuestionEntity updated = questionDao.save(toQuestionEntity(model, questionDao.findById(UUID.fromString(model.getId()))));
            QuizUtils.saveImage(updated.getPhotoFilename(), updated.getOriginalPhotoUrl());
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
        } else {
            entity.setId(UUID.randomUUID());
            entity.setCreationDate(LocalDateTime.now());
            entity.setCreatedBy(QuizUtils.getLoggedUser());
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

    private void deletePhotoFromDisk(String photoFilename) {
        if (StringUtils.isNotEmpty(photoFilename)) {
            QuizUtils.deleteImageFile(photoFilename);
        }
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
}
