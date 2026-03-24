package org.rsinitsyn.quiz.service.strategy.update;

import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.*;
import org.rsinitsyn.quiz.model.binding.AbstractQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.LinkQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.time.LocalDateTime.*;
import static org.rsinitsyn.quiz.entity.QuestionHintType.PHOTO;
import static org.rsinitsyn.quiz.entity.QuestionHintType.TEXT;
import static org.rsinitsyn.quiz.utils.QuizUtils.generateFilename;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Component
public abstract class AbstractQuestionUpdateStrategy<T extends AbstractQuestionBindingModel> implements QuestionUpdateStrategy<T> {

    protected final QuizAppProperties properties;
    protected final QuestionCategoryService categoryService;

    public AbstractQuestionUpdateStrategy(QuizAppProperties properties,
                                          QuestionCategoryService categoryService) {
        this.properties = properties;
        this.categoryService = categoryService;
    }

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
        categoryService.findByName(model.getCategory())
                .ifPresentOrElse(
                        question::setCategory,
                        () -> question.setCategory(categoryService.getOrCreateDefaultCategory()));
    }

    protected void createHook(T model, QuestionEntity question) {
        question.setCreationDate(now());
        question.setCreatedBy(getLoggedUser());
        question.setOptionsOnly(false);
        createHints(model).forEach(question::addHint);
    }

    protected void updateHook(T model, QuestionEntity question, QuestionEntity persistEntity) {
        question.setId(UUID.fromString(model.getId()));
        question.setCreationDate(persistEntity.getCreationDate());
        question.setCreatedBy(persistEntity.getCreatedBy());
        question.setOptionsOnly(persistEntity.isOptionsOnly());
        question.setAudioFilename(persistEntity.getAudioFilename());
        question.setGrades(persistEntity.getGrades());
        question.setAnswers(persistEntity.getAnswers());
        question.getHints().clear();
        createHints(model).forEach(question::addHint);
    }

    protected final AnswerEntity createAnswerEntity(String text,
                                                    boolean correct,
                                                    int number,
                                                    String photoUrl) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setText(text);
        answerEntity.setCorrect(correct);
        answerEntity.setNumber(number);
        if (photoUrl != null) {
            answerEntity.setPhotoFilename(properties.getFilesFolder() + generateFilename(photoUrl));
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
            question.setPhotoFilename(properties.getFilesFolder() + generateFilename(newPhotoUrl));
            return;
        }
        String oldPhotoUrl = oldEntity != null ? oldEntity.getOriginalPhotoUrl() : null;
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
            question.setPhotoFilename(properties.getFilesFolder() + generateFilename(newPhotoUrl));
        } else {
            question.setOriginalPhotoUrl(oldPhotoUrl);
            question.setPhotoFilename(oldEntity.getPhotoFilename());
            question.setShouldSaveImage(false);
        }
    }

    protected List<QuestionHintEntity> createHints(T model) {
        final var counter = new AtomicInteger(0);
        return model.getHintsText().lines()
                .map(line -> {
                    final var entity = new QuestionHintEntity();
                    entity.setType(TEXT);
                    entity.setText(line);
                    entity.setNumber(counter.getAndIncrement());
                    return entity;
                })
                .toList();
    }
}
