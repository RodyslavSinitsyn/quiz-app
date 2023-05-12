package org.rsinitsyn.quiz.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGradePrimaryKey implements Serializable {
    private UUID questionId;
    private UUID userId;
}
