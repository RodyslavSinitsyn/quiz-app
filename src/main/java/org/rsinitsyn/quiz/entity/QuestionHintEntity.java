package org.rsinitsyn.quiz.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.InputStream;
import java.util.UUID;

import static org.rsinitsyn.quiz.entity.QuestionHintType.PHOTO;
import static org.rsinitsyn.quiz.entity.QuestionHintType.TEXT;

@Entity
@Table(name = "question_hints")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString
public class QuestionHintEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private String text;
    private String originalPhotoUrl;
    private String photoFilename;
    @Enumerated(EnumType.STRING)
    private QuestionHintType type;
    private int number;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", referencedColumnName = "id")
    @ToString.Exclude
    private QuestionEntity question;

    @Transient
    private InputStream photoInputStream;

    public boolean photoType() {
        return type == PHOTO;
    }

    public boolean textType() {
        return type == TEXT;
    }

    @PrePersist
    @PreUpdate
    public void validate() {
        if (text == null && photoFilename == null) {
            throw new IllegalArgumentException("Question hint must have either text or photo");
        }
    }
}
