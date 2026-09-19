package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class GameParticipantId implements Serializable {
    private UUID gameId;
    private UUID userId;

    public static GameParticipantId gameParticipantId(UUID gameId, UUID userId) {
        return new GameParticipantId(gameId, userId);
    }
}
