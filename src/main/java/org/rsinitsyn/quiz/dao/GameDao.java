package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameDao extends JpaRepository<GameEntity, UUID> {

    @Query("SELECT DISTINCT ge FROM GameEntity ge LEFT JOIN FETCH ge.gameQuestions WHERE ge.id = :id")
    Optional<GameEntity> findByIdJoinQuestions(@Param("id") UUID id);

    List<GameEntity> findAllByCreatedByAndStatus(String createdBy, GameStatus status);

    int countAllByCreatedByAndStatus(String createdBy, GameStatus status);

    @Query("SELECT DISTINCT ge " +
            "FROM GameEntity ge " +
            "LEFT JOIN FETCH ge.gameQuestions gq " +
            "WHERE gq.user.username = :playerName " +
            "AND ge.status = :status")
    List<GameEntity> findAllByPlayerNameAndStatus(@Param("playerName") String playerName,
                                                  @Param("status") GameStatus status);

    @Query("SELECT DISTINCT ge " +
            "FROM GameEntity ge " +
            "LEFT JOIN FETCH ge.gameQuestions " +
            "ORDER BY ge.creationDate DESC")
    List<GameEntity> findAllJoinGamesQuestionsNewFirst();

    boolean existsById(UUID id);
}
