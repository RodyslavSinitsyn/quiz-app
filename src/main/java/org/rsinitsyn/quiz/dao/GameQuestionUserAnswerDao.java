package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.GameQuestionUserAnswerEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameQuestionUserAnswerDao extends JpaRepository<GameQuestionUserAnswerEntity, GameQuestionUserAnswerId> {
    List<GameQuestionUserAnswerEntity> findAllByIdGameIdAndIdQuestionId(UUID gameId, UUID questionId);
}
