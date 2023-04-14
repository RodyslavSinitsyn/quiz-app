package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionCategoryDao extends JpaRepository<QuestionCategoryEntity, UUID> {
    boolean existsByName(String name);
    Optional<QuestionCategoryEntity> findByName(String name);
    @Query("FROM QuestionCategoryEntity c ORDER BY c.name")
    List<QuestionCategoryEntity> findAllOrderByName();
}
