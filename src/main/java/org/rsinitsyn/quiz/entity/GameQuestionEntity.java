package org.rsinitsyn.quiz.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "games_questions",
        uniqueConstraints = @UniqueConstraint(name = "gq_gameid_ordernumber_uq", columnNames = {"game_id", "orderNumber"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id", "answered", "orderNumber"})
public class GameQuestionEntity {
    @EmbeddedId
//    @AttributeOverrides({
//            @AttributeOverride(name = "gameId", column = @Column(name = "gameId")),
//            @AttributeOverride(name = "questionId", column = @Column(name = "questionId"))
//    })
    private GameQuestionPrimaryKey id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("gameId")
    private GameEntity game;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("questionId")
    private QuestionEntity question;

    // Null - not answered yet
    // True - correct answer
    // False - incorrect answer
    private Boolean answered;
    private int orderNumber;
}
