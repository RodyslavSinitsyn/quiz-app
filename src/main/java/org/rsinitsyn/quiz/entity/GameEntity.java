package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "games")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"gameQuestions", "participants"})
@ToString
public class GameEntity {
    @Id
    private UUID id;
    private String name;
    private String createdBy;
    @Enumerated(value = EnumType.STRING)
    private GameStatus status;
    @Enumerated(value = EnumType.STRING)
    private GameType type;
    @Column(nullable = false)
    private LocalDateTime creationDate;
    private LocalDateTime finishDate;
    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @OrderBy(value = "orderNumber")
    @ToString.Exclude
    private Set<GameQuestionUserEntity> gameQuestions = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "game",
            fetch = FetchType.LAZY,
            cascade = CascadeType.REMOVE,
            orphanRemoval = true
    )
    @ToString.Exclude
    private Set<GameParticipantEntity> participants = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private GameConfiguration configuration;

    /**
     * Use new method getParticipantPlayerNames
     */
    @Deprecated(forRemoval = true)
    public Set<String> getPlayerNames() {
        return gameQuestions.stream()
                .map(e -> e.getUser().getUsername())
                .collect(Collectors.toSet());
    }

    public Set<String> getParticipantPlayerNames() {
        return participants.stream()
                .filter(p -> p.getRole() == GameParticipantRole.PLAYER)
                .map(e -> e.getUser().getUsername())
                .collect(Collectors.toSet());
    }
}
