package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class GameQuestionUserId implements Serializable {
    @Column(name = "game_id", nullable = false)
    private UUID gameId;
    @Column(name = "question_id", nullable = false)
    private UUID questionId;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
