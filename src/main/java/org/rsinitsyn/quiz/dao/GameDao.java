package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rsinitsyn.quiz.entity.GameEntity;
import org.rsinitsyn.quiz.entity.GameStatus;
import org.rsinitsyn.quiz.view.GameDetailsView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameDao extends JpaRepository<GameEntity, UUID> {

    @Query("SELECT DISTINCT ge FROM GameEntity ge LEFT JOIN FETCH ge.gameQuestions WHERE ge.id = :id")
    Optional<GameEntity> findByIdJoinQuestionsOld(@Param("id") UUID id);

    @Query("SELECT DISTINCT ge FROM GameEntity ge LEFT JOIN FETCH ge.questions WHERE ge.id = :id")
    Optional<GameEntity> findByIdJoinQuestions(@Param("id") UUID id);

    @Query("SELECT DISTINCT ge FROM GameEntity ge LEFT JOIN FETCH ge.participants WHERE ge.id = :id")
    Optional<GameEntity> findByIdJoinParticipants(@Param("id") UUID id);

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
    List<GameEntity> findAllJoinGamesQuestionsNewFirstOld();

    @Query("SELECT DISTINCT ge " +
            "FROM GameEntity ge " +
            "JOIN FETCH ge.questions " +
            "ORDER BY ge.creationDate DESC")
    List<GameEntity> findAllJoinGamesQuestionsNewFirst();

    @Query("SELECT DISTINCT ge " +
            "FROM GameEntity ge " +
            "LEFT JOIN FETCH ge.participants " +
            "ORDER BY ge.creationDate DESC")
    List<GameEntity> findAllJoinParticipantsNewFirst();

    boolean existsById(UUID id);

    @Query(value = """
            WITH question_stats AS (
                SELECT
                    gq.game_id,
                    COUNT(*) AS total_questions
                FROM game_questions gq
                GROUP BY gq.game_id
            ),
            answer_stats AS (
                SELECT
                    a.game_id,
                    COUNT(DISTINCT a.question_id)
                        FILTER (WHERE a.status <> 'UNKNOWN') AS answered_questions,
                    COUNT(DISTINCT a.question_id)
                        FILTER (WHERE a.status = 'CORRECT') AS correct_answers
                FROM game_question_user_answers a
                GROUP BY a.game_id
            ),
            player_stats AS (
                SELECT
                    gp.game_id,
                    ARRAY_AGG(DISTINCT u.username)
                        FILTER (WHERE gp.role = 'PLAYER') AS player_names
                FROM game_participants gp
                JOIN users_tbl u ON u.id = gp.user_id
                GROUP BY gp.game_id
            )
            SELECT
                g.id,
                g.status,
                g.created_by,
                g.type,
                g.name,
            
                COALESCE(ps.player_names, ARRAY[]::varchar[]) AS player_names,
            
                COALESCE(ans.answered_questions, 0) AS answered_questions,
                COALESCE(qs.total_questions, 0) AS total_questions,
                COALESCE(ans.correct_answers, 0) AS correct_answers,
            
                g.creation_date,
                g.finish_date
            
            FROM games g
            
            LEFT JOIN question_stats qs
                   ON qs.game_id = g.id
            
            LEFT JOIN answer_stats ans
                   ON ans.game_id = g.id
            
            LEFT JOIN player_stats ps
                   ON ps.game_id = g.id
            
            ORDER BY g.creation_date DESC
            """,
            nativeQuery = true)
    List<GameDetailsView> findAllGameDetails();
}
