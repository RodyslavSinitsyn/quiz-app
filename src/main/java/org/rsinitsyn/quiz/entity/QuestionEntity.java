package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Formula;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class QuestionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String text;
    @Enumerated(EnumType.STRING)
    private QuestionType type;
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime creationDate;
    private String originalPhotoUrl;
    private String photoFilename;
    private String audioFilename;
    private boolean optionsOnly;
    private Integer validRange;
    private String answerDescriptionText;
    private String answerDescriptionPhotoFilename;  // TODO For now not used
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private QuestionCategoryEntity category;

//    @Formula("SELECT count(*) FROM games_questions gq WHERE gq.question_id = id")
    @Transient
    private long gamesQuestionsCount;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number")
    @ToString.Exclude
    private Set<AnswerEntity> answers = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<QuestionGrade> grades = new HashSet<>();

    @Transient
    private Set<String> resourcesToDelete = new HashSet<>();

    @Transient
    private boolean shouldSaveImage = true;

    public void addAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(this);
        answers.add(answerEntity);
    }

    public void removeAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(null);
        answers.remove(answerEntity);
    }

    public double getGradeValue() {
        return QuizUtils.divide(
                getGrades().stream().mapToInt(QuestionGrade::getGrade).sum(),
                getGrades().size(),
                1);
    }

    public boolean presentInAnyGame() {
        return gamesQuestionsCount > 0;
    }
}
