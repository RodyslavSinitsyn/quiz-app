package org.rsinitsyn.quiz.properties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "quiz")
@Data
public class QuizAppProperties {
    private String filesFolder;
    private OpenAiProperties openai;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OpenAiProperties {
        private String apiKey;
        private String url;
        private String model;
    }
}
