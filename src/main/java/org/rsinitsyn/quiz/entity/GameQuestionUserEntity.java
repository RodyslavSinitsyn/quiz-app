package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "games_questions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "answered", "orderNumber"})
@ToString
public class GameQuestionUserEntity {
    @EmbeddedId
    private GameQuestionUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "game_id")
    @ToString.Exclude
    private GameEntity game;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    @ToString.Exclude
    private QuestionEntity question;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_result")
    private AnswerStatus answerStatus;
    private String answerText;
    @Getter(AccessLevel.NONE)
    private Boolean answered;
    private int orderNumber;

    public Boolean getAnswered() {
        return this.answerStatus.boolVal;
    }
}
