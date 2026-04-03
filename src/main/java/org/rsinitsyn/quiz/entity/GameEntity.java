package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
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
@EqualsAndHashCode(exclude = {"gameQuestions"})
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

    public Set<String> getPlayerNames() {
        return gameQuestions.stream()
                .map(e -> e.getUser().getUsername())
                .collect(Collectors.toSet());
    }
}
