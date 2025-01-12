package org.rsinitsyn.quiz.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
    @MapsId("game_id")
    @JoinColumn(name = "game_id")
    @ToString.Exclude
    private GameEntity game;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("question_id")
    @JoinColumn(name = "question_id")
    @ToString.Exclude
    private QuestionEntity question;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("user_id")
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserEntity user;

    // Null - not answered yet
    // True - correct answer
    // False - incorrect answer
    private Boolean answered;
    private String answerText;
    private int orderNumber;
}
