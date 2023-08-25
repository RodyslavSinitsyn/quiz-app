package org.rsinitsyn.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.service.ai.AIResponseParserService;

@ExtendWith(MockitoExtension.class)
class AIResponseParserServiceTest {

    private AIResponseParserService parserService;
    @Mock
    private QuestionService mockQuestionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        parserService = new AIResponseParserService(
                objectMapper,
                mockQuestionService
        );
    }

    @Test
    void saveQuestionsFromAI() {
        // given
        String json = """
                {
                  "questions": [
                    {
                      "questionText": "Какое химическое вещество содержится в лимонах?",
                      "categoryName": "Химия",
                      "answers": ["Витамин С", "Кальций", "Йод", "Магний"],
                      "correctOption": 1
                    },
                    {
                      "questionText": "Кто из перечисленных является основателем теории относительности?",
                      "categoryName": "Физика",
                      "answers": ["Альберт Эйнштейн", "Никола Тесла", "Исаак Ньютон", "Макс Планк"],
                      "correctOption": 0
                    },
                    {
                      "questionText": "Какой город является столицей Канады?",
                      "categoryName": "География",
                      "answers": ["Торонто", "Оттава", "Ванкувер", "Монреаль"],
                      "correctOption": 1
                    }
                  ]
                }
                """;

        // when
        parserService.saveQuestionsFromAI(json);

        // then
        Mockito.verify(mockQuestionService, Mockito.times(3)).saveEntityAndImage(Mockito.any(QuestionEntity.class));
    }
}