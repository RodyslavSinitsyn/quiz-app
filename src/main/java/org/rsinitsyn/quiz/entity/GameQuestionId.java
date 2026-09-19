package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@RequiredArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class GameQuestionId implements Serializable {
    @Column(name = "game_id", nullable = false)
    private final UUID gameId;
    @Column(name = "question_id", nullable = false)
    private final UUID questionId;

    public static GameQuestionId gameQuestionId(UUID gameId, UUID questionId) {
        return new GameQuestionId(gameId, questionId);
    }
}
