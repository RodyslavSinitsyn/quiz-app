package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionDao extends JpaRepository<QuestionEntity, UUID> {
    @Query("from QuestionEntity q join fetch q.answers")
    public List<QuestionEntity> findAllWithAnswers();

    List<QuestionEntity> findAllByCreatedBy(String createdBy);
}
