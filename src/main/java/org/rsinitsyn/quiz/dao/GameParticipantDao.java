package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.GameParticipantEntity;
import org.rsinitsyn.quiz.entity.GameParticipantId;
import org.rsinitsyn.quiz.entity.GameParticipantRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameParticipantDao extends JpaRepository<GameParticipantEntity, GameParticipantId> {
    long countByGameIdAndRole(UUID gameId, GameParticipantRole role);

    List<GameParticipantEntity> findByGameIdAndRole(UUID gameId, GameParticipantRole role);
}
