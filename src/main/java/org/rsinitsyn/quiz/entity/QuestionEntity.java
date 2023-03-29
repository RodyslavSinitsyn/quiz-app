package org.rsinitsyn.quiz.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = "answers")
public class QuestionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, columnDefinition = "CHARACTER VARYING(1000)")
    private String text;
    @Enumerated(EnumType.STRING)
    private QuestionType type;
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime creationDate;
    @Column(columnDefinition = "CHARACTER VARYING(1000)")
    private String originalPhotoUrl;
    private String photoFilename;
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "categoryId", referencedColumnName = "id")
    private QuestionCategoryEntity category;
    @OneToMany(fetch = FetchType.EAGER, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AnswerEntity> answers = new HashSet<>();

    public void addAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(this);
        answers.add(answerEntity);
    }

    public void removeAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(null);
        answers.remove(answerEntity);
    }
}
