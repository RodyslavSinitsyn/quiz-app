package org.rsinitsyn.quiz.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AIResponseParserService {

    private final ObjectMapper objectMapper;
    private final QuestionService questionService;

    public void saveQuestionsFromAI(String response) {
        try {
            ArrayNode jsonArray = (ArrayNode) objectMapper.readTree(response).get("questions");
            List<QuestionEntity> questions = new ArrayList<>();
            for (JsonNode jsonNode : jsonArray) {
                questions.add(toQuestionEntity(jsonNode));
            }
            questions.forEach(questionService::saveEntityAndImage);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private QuestionEntity toQuestionEntity(JsonNode jsonNode) {
        QuestionEntity question = new QuestionEntity();
        question.setCreatedBy(SessionWrapper.getLoggedUser());
        question.setType(QuestionType.TEXT);
        question.setOptionsOnly(true);
        question.setCreationDate(LocalDateTime.now());
        question.setText(jsonNode.get("questionText").asText());

        var arrAnswers = (ArrayNode) jsonNode.get("answers");
        for (int index = 0; index < arrAnswers.size(); index++) {
            var answerText = arrAnswers.get(index).asText();
            AnswerEntity answerEntity = new AnswerEntity();
            answerEntity.setText(answerText);
            answerEntity.setNumber(index);
            answerEntity.setCorrect(index == jsonNode.get("correctOption").asInt());

            question.addAnswer(answerEntity);
        }

        String categoryName = jsonNode.get("categoryName").asText();
        var persistentCategory = questionService.findCategoryByName(categoryName)
                .orElseGet(() -> questionService.saveQuestionCategory(categoryName));
        question.setCategory(persistentCategory);

        return question;
    }

}
