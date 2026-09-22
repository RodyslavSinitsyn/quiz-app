package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameQuestionDao extends JpaRepository<GameQuestionEntity, GameQuestionId> {
    List<GameQuestionEntity> findByGameId(UUID gameId);
}
