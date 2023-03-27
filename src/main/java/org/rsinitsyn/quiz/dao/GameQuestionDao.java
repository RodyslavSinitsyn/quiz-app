package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameQuestionDao extends JpaRepository<GameQuestionEntity, GameQuestionPrimaryKey> {
}
