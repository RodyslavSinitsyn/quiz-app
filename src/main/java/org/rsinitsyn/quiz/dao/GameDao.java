package org.rsinitsyn.quiz.dao;

import java.util.UUID;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameDao extends JpaRepository<GameEntity, UUID> {
}
