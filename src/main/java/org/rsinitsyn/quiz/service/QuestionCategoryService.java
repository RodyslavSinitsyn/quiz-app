package org.rsinitsyn.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionCategoryService {

    public static final String GENERAL_CATEGORY = "Общие";

    private final QuestionCategoryDao questionCategoryDao;

    @Cacheable(value = "allCategories")
    public List<QuestionCategoryEntity> findAllCategories() {
        return questionCategoryDao.findAllOrderByName();
    }

    public Optional<QuestionCategoryEntity> findByName(String name) {
        return questionCategoryDao.findByName(name);
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
    public QuestionCategoryEntity save(String categoryName) {
        QuestionCategoryEntity entity = new QuestionCategoryEntity();
        entity.setName(categoryName);
        QuestionCategoryEntity saved = questionCategoryDao.save(entity);
        log.info("Category saved, name: {}", saved.getName());
        return saved;
    }
}
