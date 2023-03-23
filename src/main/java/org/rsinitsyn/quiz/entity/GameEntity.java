package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "games")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
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
    private Integer questionsCount;
    private Integer result;
    @Column(nullable = false)
    private LocalDateTime creationDate;
}
