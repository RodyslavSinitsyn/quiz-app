package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@EqualsAndHashCode
public class GameParticipantId implements Serializable {
    private final UUID gameId;
    private final UUID userId;

    private GameParticipantId(UUID gameId, UUID userId) {
        this.gameId = gameId;
        this.userId = userId;
    }

    public static GameParticipantId gameParticipantId(UUID gameId, UUID userId) {
        return new GameParticipantId(gameId, userId);
    }
}
