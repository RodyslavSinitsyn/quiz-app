package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.QuestionHintEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface QuestionHintDao extends JpaRepository<QuestionHintEntity, UUID> {
}
