package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "game_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_game_player",
                columnNames = {"game_id", "user_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class GameParticipantEntity {

    @EmbeddedId
    private GameParticipantId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private GameParticipantRole role;

    @Column(name = "join_date", nullable = false)
    private LocalDateTime joinDate; // todo: make optional

    @Column(name = "leave_date", nullable = true)
    private LocalDateTime leaveDate;
}
