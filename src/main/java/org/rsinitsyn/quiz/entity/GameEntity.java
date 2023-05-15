package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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
    @OneToMany(mappedBy = "game", fetch = FetchType.LAZY)
    @OrderBy(value = "orderNumber")
    @ToString.Exclude
    private Set<GameQuestionUserEntity> gameQuestions = new HashSet<>();

    public Set<String> getPlayerNames() {
        return gameQuestions.stream()
                .map(e -> e.getUser().getUsername())
                .collect(Collectors.toSet());
    }
}
