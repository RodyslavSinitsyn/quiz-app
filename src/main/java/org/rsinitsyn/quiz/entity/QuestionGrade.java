package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "question_grades")
@IdClass(QuestionGradeId.class)
@Getter
@Setter
@EqualsAndHashCode(exclude = "user")
@ToString
public class QuestionGrade {
    @Id
    @Column(name = "question_id")
    private UUID questionId;
    @Id
    @Column(name = "user_id")
    private UUID userId;
    @Min(value = 1)
    @Max(value = 5)
    private int grade;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("question_id")
    @JoinColumn(name = "question_id")
    @ToString.Exclude
    private QuestionEntity question;
    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("user_id")
    @JoinColumn(name = "user_id")
    @ToString.Exclude
    private UserEntity user;
}
