package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "game_question_user_answers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_game_question_user_answer",
                        columnNames = {"game_id", "question_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
public class GameQuestionUserAnswerEntity {

    @EmbeddedId
    private GameQuestionUserAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = "game_id",
                    referencedColumnName = "game_id",
                    insertable = false,
                    updatable = false
            ),
            @JoinColumn(
                    name = "question_id",
                    referencedColumnName = "question_id",
                    insertable = false,
                    updatable = false
            )
    })
    @ToString.Exclude
    private GameQuestionEntity gameQuestion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(
                    name = "game_id",
                    referencedColumnName = "game_id",
                    insertable = false,
                    updatable = false
            ),
            @JoinColumn(
                    name = "user_id",
                    referencedColumnName = "user_id",
                    insertable = false,
                    updatable = false
            )
    })
    @ToString.Exclude
    private GameParticipantEntity participant;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status", nullable = false)
    private AnswerStatus answerStatus;

    @Column(name = "answer_text")
    private String answerText;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;
}
