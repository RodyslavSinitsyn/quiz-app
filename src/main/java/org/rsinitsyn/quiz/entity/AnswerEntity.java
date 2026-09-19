package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
    private String photoFilename;
    @Transient
    private String photoUrl;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    @ToString.Exclude
    private QuestionEntity question;

    public AnswerEntity(String text, boolean correct, int number) {
        this.text = text;
        this.correct = correct;
        this.number = number;
    }
}
