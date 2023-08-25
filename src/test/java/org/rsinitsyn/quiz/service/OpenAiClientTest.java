package org.rsinitsyn.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rsinitsyn.quiz.dto.ChatRequestDto;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.ai.OpenAiClient;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class OpenAiClientTest {

    private OpenAiClient client;
    @Mock
    private RestTemplate mockRestTemplate;
    @Mock
    private QuizAppProperties mockProps;
    private final ObjectMapper objectMapper = new JsonMapper();

    @BeforeEach
    void setUp() {
        client = new OpenAiClient(
                mockRestTemplate,
                mockProps,
                objectMapper
        );

        QuizAppProperties.OpenAiProperties mockOpenAiProps = new QuizAppProperties.OpenAiProperties("apiKey", "url", "chat-gpt");
        Mockito.when(mockProps.getOpenai()).thenReturn(mockOpenAiProps);
    }

    @Test
    void fetchQuestions() {
        // given
        String stubResponse = """
                {
                  "id": "chatcmpl-6p9XYPYSTTRi0xEviKjjilqrWU2Ve",
                  "object": "chat.completion",
                  "created": 1677649420,
                  "model": "text-davinci-003",
                  "usage": {
                    "prompt_tokens": 56,
                    "completion_tokens": 31,
                    "total_tokens": 87
                  },
                  "choices": [
                    {
                      "message": {
                        "role": "assistant",
                        "content": "The capital city of Australia is Canberra."
                      },
                      "finish_reason": "stop",
                      "index": 0
                    }
                  ]
                }
                """;
        Mockito.when(mockRestTemplate.postForObject(
                        Mockito.anyString(),
                        Mockito.any(ChatRequestDto.class),
                        Mockito.eq(String.class)
                ))
                .thenReturn(stubResponse);

        // when
        String actual = client.fetchQuestions("anything");

        // then
        Assertions.assertEquals("The capital city of Australia is Canberra.", actual);
    }
}