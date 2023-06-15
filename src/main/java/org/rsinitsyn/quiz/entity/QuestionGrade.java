package org.rsinitsyn.quiz.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
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
    private UUID questionId;
    @Id
    private UUID userId;
    @Min(value = 1)
    @Max(value = 5)
    private int grade;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "questionId")
    @ToString.Exclude
    private QuestionEntity question;
    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "userId")
    @ToString.Exclude
    private UserEntity user;
}
