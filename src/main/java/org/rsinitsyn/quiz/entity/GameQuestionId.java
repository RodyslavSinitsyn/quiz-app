package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class GameQuestionId implements Serializable {
    @Column(name = "game_id", nullable = false)
    private UUID gameId;
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    public static GameQuestionId gameQuestionId(UUID gameId, UUID questionId) {
        return new GameQuestionId(gameId, questionId);
    }
}
