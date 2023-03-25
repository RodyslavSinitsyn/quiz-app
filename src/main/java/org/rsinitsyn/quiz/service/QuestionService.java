package org.rsinitsyn.quiz.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.utils.QuizResourceUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionService {

    private final QuestionDao questionDao;
    private final QuestionCategoryDao questionCategoryDao;

    public void saveQuestionCategory(QuestionCategoryBindingModel model) {
        QuestionCategoryEntity entity = new QuestionCategoryEntity();
        entity.setName(model.getCategoryName());
        QuestionCategoryEntity saved = questionCategoryDao.save(entity);
        log.info("Category saved, name: {}", saved.getName());
    }

    public boolean categoryExists(String categoryName) {
        return questionCategoryDao.existsByName(categoryName);
    }

    public List<QuestionEntity> findAll() {
        return questionDao.findAll();
    }

    public List<QuestionEntity> findAllByCurrentUser() { // Todo temp solution replace with FILTERS
        String loggedUser = QuizResourceUtils.getLoggedUser();
        return questionDao.findAll()
                .stream().filter(entity -> entity.getCreatedBy().equals(loggedUser))
                .toList();
    }

    public void saveAll(Collection<QuestionEntity> entities) {
        questionDao.saveAll(entities);
    }

    public void saveOrUpdate(QuestionEntity entity) {
        questionDao.save(entity);
    }

    public void deleteById(String id) {
        questionDao.deleteById(UUID.fromString(id));
    }

    public List<QuestionCategoryEntity> findAllCategories() {
        return questionCategoryDao.findAll();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void saveOrUpdate(FourAnswersQuestionBindingModel model) {
        if (model.getId() == null) {
            QuestionEntity saved = questionDao.save(toQuestionEntity(model, Optional.empty()));
            log.info("Question saved: {}", saved);
        } else {
            QuestionEntity updated = questionDao.save(toQuestionEntity(model, questionDao.findById(UUID.fromString(model.getId()))));
            log.info("Question updated: {}", updated);
        }
    }

    public QuestionEntity toQuestionEntity(FourAnswersQuestionBindingModel model, Optional<QuestionEntity> optEntity) {
        boolean update = optEntity.isPresent();

        QuestionEntity entity = new QuestionEntity();

        if (update) {
            entity.setId(UUID.fromString(model.getId()));
            entity.setCreationDate(optEntity.get().getCreationDate());
            entity.setCreatedBy(optEntity.get().getCreatedBy());
        } else {
            entity.setId(UUID.randomUUID());
            entity.setCreationDate(LocalDateTime.now());
            entity.setCreatedBy(QuizResourceUtils.getLoggedUser());
        }

        entity.setText(model.getText());

        entity.addAnswer(createAnswerEntity(model.getCorrectAnswerText(), true));
        entity.addAnswer(createAnswerEntity(model.getSecondOptionAnswerText(), false));
        entity.addAnswer(createAnswerEntity(model.getThirdOptionAnswerText(), false));
        entity.addAnswer(createAnswerEntity(model.getFourthOptionAnswerText(), false));

        if (StringUtils.isNotBlank(model.getPhotoLocation())) {
            entity.setType(QuestionType.PHOTO);
            String photoFilename = QuizResourceUtils.saveImageAndGetFilename(model.getPhotoLocation());
            entity.setPhotoFilename(photoFilename);
        } else {
            entity.setType(QuestionType.TEXT);
            entity.setPhotoFilename(null);
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

    private AnswerEntity createAnswerEntity(String text, boolean correct) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setText(text);
        answerEntity.setCorrect(correct);
        return answerEntity;
    }

    public QuestionCategoryEntity getOrCreateDefaultCategory() {
        final String defaultName = "Общие";
        return questionCategoryDao.findByName(defaultName)
                .orElseGet(() -> {
                    QuestionCategoryEntity categoryEntity = new QuestionCategoryEntity();
                    categoryEntity.setName("Общие");
                    return questionCategoryDao.save(categoryEntity);
                });

    }
}
