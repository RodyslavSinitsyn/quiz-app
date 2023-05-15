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

@Entity
@Table(name = "games_questions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "answered", "orderNumber"})
public class GameQuestionUserEntity {
    @EmbeddedId
    private GameQuestionUserId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    @JoinColumn(name = "gameId")
    private GameEntity game;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("questionId")
    @JoinColumn(name = "questionId")
    private QuestionEntity question;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "userId")
    private UserEntity user;

    // Null - not answered yet
    // True - correct answer
    // False - incorrect answer
    private Boolean answered;
    private String answerText;
    private int orderNumber;
}
