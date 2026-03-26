package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    @Column(nullable = false)
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

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number")
    @ToString.Exclude
    private List<AnswerEntity> answers = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<QuestionGrade> grades = new ArrayList<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("number")
    @ToString.Exclude
    private List<QuestionHintEntity> hints = new ArrayList<>();

    @Transient
    private Set<String> resourcesToDelete = new HashSet<>();

    @Transient
    private boolean shouldSaveImage = true;

//    @Formula("SELECT count(*) FROM games_questions gq WHERE gq.question_id = id")
    @Transient
    private long gamesQuestionsCount;

    public String getTextTruncated(int maxWidth) {
        return StringUtils.truncate(text, maxWidth);
    }

    public void addAnswer(AnswerEntity answerEntity) {
        answerEntity.setQuestion(this);
        answers.add(answerEntity);
    }

    public AnswerEntity getCorrectAnswer() {
        return answers.stream()
                .filter(AnswerEntity::isCorrect)
                .findFirst()
                .orElseThrow();
    }

    public String getAnswersAsText() {
        return answers.stream()
                .map(AnswerEntity::getText)
                .collect(Collectors.joining(", "));
    }

    public void addHint(QuestionHintEntity hintEntity) {
        hintEntity.setQuestion(this);
        hints.add(hintEntity);
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

    public String getHintsAsText() {
        return hints.stream()
                .map(QuestionHintEntity::getText)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    public boolean presentInAnyGame() {
        return gamesQuestionsCount > 0;
    }
}
