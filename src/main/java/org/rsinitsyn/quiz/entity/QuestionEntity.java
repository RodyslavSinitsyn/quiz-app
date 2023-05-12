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
import jakarta.persistence.OrderBy;
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
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"answers"})
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
    private String audioFilename;
    @Column(columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean optionsOnly;
    private Integer validRange;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoryId", referencedColumnName = "id")
    private QuestionCategoryEntity category;

    @Formula("SELECT count(*) FROM games_questions gq WHERE gq.questionId = id")
    private long gamesQuestionsCount;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number")
    private Set<AnswerEntity> answers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<QuestionGrade> grades = new HashSet<>();

    public void addAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(this);
        answers.add(answerEntity);
    }

    public void removeAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(null);
        answers.remove(answerEntity);
    }

    public boolean presentInAnyGame() {
        return gamesQuestionsCount > 0;
    }
}
