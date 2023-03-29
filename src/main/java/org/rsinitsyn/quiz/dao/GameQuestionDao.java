package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.GameQuestionEntity;
import org.rsinitsyn.quiz.entity.GameQuestionPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameQuestionDao extends JpaRepository<GameQuestionEntity, GameQuestionPrimaryKey> {

    @Query("from GameQuestionEntity gqe WHERE gqe.question.id in (:questionIds)")
    List<GameQuestionEntity> findAllByQuestionIdIn(@Param("questionIds") Iterable<UUID> questionIds);
}
