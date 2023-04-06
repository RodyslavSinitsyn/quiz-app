package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameQuestionDao extends JpaRepository<GameQuestionUserEntity, GameQuestionUserPrimaryKey> {

    @Query("from GameQuestionUserEntity gqe WHERE gqe.question.id in (:questionIds)")
    List<GameQuestionUserEntity> findAllByQuestionIdIn(@Param("questionIds") Iterable<UUID> questionIds);
}
