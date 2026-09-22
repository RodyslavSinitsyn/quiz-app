package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.entity.GameParticipantRole;
import org.rsinitsyn.quiz.entity.GameQuestionUserAnswerEntity;
import org.rsinitsyn.quiz.entity.GameQuestionUserAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameQuestionUserAnswerDao extends JpaRepository<GameQuestionUserAnswerEntity, GameQuestionUserAnswerId> {
    List<GameQuestionUserAnswerEntity> findAllByIdGameIdAndIdQuestionId(UUID gameId, UUID questionId);

    List<GameQuestionUserAnswerEntity> findAllByIdGameIdAndIdUserId(UUID gameId, UUID userId);

    @Query("""
        select count(a)
        from GameQuestionUserAnswerEntity a
        join a.participant p
        where a.id.gameId = :gameId
          and a.id.questionId = :questionId
          and p.role = :role
          and a.status <> :status
        """)
    long countAnsweredPlayers(UUID gameId, UUID questionId, GameParticipantRole role, AnswerStatus status);
}
