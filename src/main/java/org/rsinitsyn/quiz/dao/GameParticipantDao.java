package org.rsinitsyn.quiz.dao;

import org.rsinitsyn.quiz.entity.GameParticipantEntity;
import org.rsinitsyn.quiz.entity.GameParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameParticipantDao extends JpaRepository<GameParticipantEntity, GameParticipantId> {
}
