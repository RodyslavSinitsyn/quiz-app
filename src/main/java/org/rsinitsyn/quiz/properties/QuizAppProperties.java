package org.rsinitsyn.quiz.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quiz")
@Data
public class QuizAppProperties {
    private String audioFolder;
}
