package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuestionDao extends JpaRepository<QuestionEntity, UUID> {
    @Query("from QuestionEntity q join fetch AnswerEntity")
    public List<QuestionEntity> findAllWithAnswers();
}
