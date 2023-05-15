package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "answers")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class AnswerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String text;
    private boolean correct;
    private int number;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "questionId", referencedColumnName = "id")
    @ToString.Exclude
    private QuestionEntity question;

    public AnswerEntity(String text, boolean correct, int number) {
        this.text = text;
        this.correct = correct;
        this.number = number;
    }
}
