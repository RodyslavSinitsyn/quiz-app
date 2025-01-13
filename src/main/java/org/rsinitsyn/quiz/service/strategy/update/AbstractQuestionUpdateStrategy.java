package org.rsinitsyn.quiz.service.strategy.update;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.binding.AbstractQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public abstract class AbstractQuestionUpdateStrategy<T extends AbstractQuestionBindingModel> implements QuestionUpdateStrategy<T> {
    public static final String GENERAL_CATEGORY = "Общие";

    protected final QuizAppProperties properties;
    protected final QuestionCategoryDao questionCategoryDao;

    public abstract void setType(T model, QuestionEntity question);

    @Override
    public QuestionEntity prepareEntity(T bindingModel, QuestionEntity persistEntity) {
        return toQuestionEntity(bindingModel, persistEntity);
    }

    private QuestionEntity toQuestionEntity(T model,
                                            QuestionEntity persistEntity) {
        boolean update = persistEntity != null;
        QuestionEntity newEntity = new QuestionEntity();
        setType(model, newEntity);
        commonHook(model, newEntity, persistEntity);
        if (update) {
            updateHook(model, newEntity, persistEntity);
        } else {
            createHook(model, newEntity);
        }
        return newEntity;
    }

    protected void commonHook(T model, QuestionEntity question, QuestionEntity persistEntity) {
        question.setText(model.getText());
        question.setAnswerDescriptionText(model.getAnswerDescriptionText());
        setPhotoFields(question, persistEntity, model.getPhotoLocation());
        questionCategoryDao.findByName(model.getCategory())
                .ifPresentOrElse(
                        question::setCategory,
                        () -> {
                            QuestionCategoryEntity defaultCategory = getOrCreateDefaultCategory();
                            question.setCategory(defaultCategory);
                        });
    }

    protected void createHook(T model, QuestionEntity question) {
        question.setId(UUID.randomUUID());
        question.setCreationDate(LocalDateTime.now());
        question.setCreatedBy(SessionWrapper.getLoggedUser());
        question.setOptionsOnly(false);
    }

    protected void updateHook(T model, QuestionEntity question, QuestionEntity persistEntity) {
        question.setId(UUID.fromString(model.getId()));
        question.setCreationDate(persistEntity.getCreationDate());
        question.setCreatedBy(persistEntity.getCreatedBy());
        question.setOptionsOnly(persistEntity.isOptionsOnly());
        question.setAudioFilename(persistEntity.getAudioFilename());
        question.setGrades(persistEntity.getGrades());
        question.setAnswers(persistEntity.getAnswers());
    }

    protected AnswerEntity createAnswerEntity(String text,
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

    protected void setPhotoFields(QuestionEntity question,
                                  QuestionEntity oldEntity,
                                  String newPhotoUrl) {
        if (oldEntity == null && StringUtils.isNotEmpty(newPhotoUrl)) {
            question.setOriginalPhotoUrl(newPhotoUrl);
            question.setPhotoFilename(properties.getFilesFolder() + QuizUtils.generateFilename(newPhotoUrl));
            return;
        }
        String oldPhotoUrl = oldEntity.getOriginalPhotoUrl();
        if (StringUtils.isEmpty(newPhotoUrl)) {
            if (StringUtils.isNotEmpty(oldPhotoUrl)) {
                question.getResourcesToDelete().add(oldEntity.getPhotoFilename());
                question.setPhotoFilename(null);
                question.setOriginalPhotoUrl(null);
            }
        } else if (!StringUtils.equals(oldPhotoUrl, newPhotoUrl)) {
            if (StringUtils.isNotEmpty(oldPhotoUrl)) {
                question.getResourcesToDelete().add(oldEntity.getPhotoFilename());
            }
            question.setOriginalPhotoUrl(newPhotoUrl);
            question.setPhotoFilename(properties.getFilesFolder() + QuizUtils.generateFilename(newPhotoUrl));
        } else {
            question.setOriginalPhotoUrl(oldPhotoUrl);
            question.setPhotoFilename(oldEntity.getPhotoFilename());
            question.setShouldSaveImage(false);
        }
    }

    protected QuestionCategoryEntity getOrCreateDefaultCategory() {
        return questionCategoryDao.findByName(GENERAL_CATEGORY)
                .orElseGet(() -> {
                    QuestionCategoryEntity categoryEntity = new QuestionCategoryEntity();
                    categoryEntity.setName(GENERAL_CATEGORY);
                    return questionCategoryDao.save(categoryEntity);
                });
    }

}
