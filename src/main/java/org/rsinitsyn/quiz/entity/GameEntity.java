package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static jakarta.persistence.CascadeType.REMOVE;
import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "games")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
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
    @OneToMany(mappedBy = "game", fetch = LAZY, cascade = REMOVE)
    @OrderBy(value = "orderNumber")
    @ToString.Exclude
    private Set<GameQuestionUserEntity> gameQuestions = new LinkedHashSet<>();

    @OneToMany(
            mappedBy = "game",
            fetch = LAZY,
            cascade = REMOVE,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<GameParticipantEntity> participants = new ArrayList<>();

    @OneToMany(
            mappedBy = "game",
            fetch = LAZY,
            cascade = REMOVE,
            orphanRemoval = true
    )
    @ToString.Exclude
    private List<GameQuestionEntity> questions = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private GameConfiguration configuration;

    @Deprecated
    public Set<GameQuestionUserEntity> getGameQuestions() {
        return gameQuestions;
    }

    public boolean oldGameQuestionsPresent() {
        return !gameQuestions.isEmpty();
    }

    /**
     * Use new method getParticipantPlayerNames
     */
    public Set<String> getPlayerNames() {
        return gameQuestions.stream()
                .map(e -> e.getUser().getUsername())
                .collect(Collectors.toSet());
    }

    public Set<String> getParticipantPlayerNames() {
        return participants.stream()
                .filter(p -> p.getRole() == GameParticipantRole.PLAYER)
                .map(e -> "[new]" + e.getUser().getUsername())
                .collect(Collectors.toSet());
    }
}
