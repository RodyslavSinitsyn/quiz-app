package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.GameQuestionUserEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameQuestionUserDao extends JpaRepository<GameQuestionUserEntity, GameQuestionUserId> {

    @Query("FROM GameQuestionUserEntity gqe WHERE gqe.question.id in (:questionIds)")
    List<GameQuestionUserEntity> findAllByQuestionIdIn(@Param("questionIds") Iterable<UUID> questionIds);

    @Query(value = "SELECT MAX(gque.orderNumber) FROM GameQuestionUserEntity gque " +
            "WHERE gque.id.gameId=:gameId")
    int getMaxOrderNumber(@Param("gameId") UUID gameId);
}
