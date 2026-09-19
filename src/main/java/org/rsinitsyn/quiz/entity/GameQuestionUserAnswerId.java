package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GameQuestionUserAnswerId implements Serializable {
    @Column(name = "game_id")
    private UUID gameId;
    @Column(name = "question_id")
    private UUID questionId;
    @Column(name = "user_id")
    private UUID userId;
}
