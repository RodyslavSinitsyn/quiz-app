package org.rsinitsyn.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.rsinitsyn.quiz.dto.ChatRequestDto;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OpenAiClient {
    private final RestTemplate openaiRestTemplate;
    private final QuizAppProperties properties;
    private final ObjectMapper objectMapper;

    public String fetchQuestions(String prompt) {
        var request = new ChatRequestDto(properties.getOpenai().getModel(), prompt);

        String response = openaiRestTemplate.postForObject(
                properties.getOpenai().getUrl(),
                request,
                String.class
        );

        return Optional.ofNullable(response)
                .map(this::readTreeWrapped)
                .map(node -> (ArrayNode)node.get("choices"))
                .map(arrNode -> arrNode.get(0).get("message").get("content").asText())
                .orElseThrow(() -> new RuntimeException("Can not parse OpenAI response."));
    }

    @SneakyThrows
    private JsonNode readTreeWrapped(String val) {
        return objectMapper.readTree(val);
    }
}
