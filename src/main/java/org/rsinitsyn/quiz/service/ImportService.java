package org.rsinitsyn.quiz.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImportService {

    private static final String DEFAULT_DELIMITER = "|";

    private final QuestionService questionService;

    @SneakyThrows
    public void importQuestions(InputStream fileContent) {
        String data = IOUtils.toString(fileContent, StandardCharsets.UTF_8);
        IOUtils.close(fileContent);
        String[] lines = data.split("\n");
        if (lines.length == 0) {
            throw new RuntimeException("Невалидный формат импортируемого файла");
        }
        List<QuestionEntity> questionEntities = Arrays.stream(lines).map(this::toEntity).toList();
        questionEntities.forEach(questionService::saveEntityAndImage);
    }

    private QuestionEntity toEntity(String line) {
        String[] tokens = line.split("[|]");

        if (tokens.length < 5) {
            throw new RuntimeException("Невалидный формат импортируемого файла");
        }

        QuestionEntity entity = new QuestionEntity();
        entity.setText(tokens[0]);

        for (int i = 0; i < 4; i++) {
            Pair<String, Boolean> parsed = parseToken(tokens[i + 1]);
            entity.addAnswer(new AnswerEntity(parsed.getFirst(), parsed.getSecond(), i));
        }

        long correctAnswersCount = entity.getAnswers().stream().filter(AnswerEntity::isCorrect).count();
        if (correctAnswersCount == 0) {
            throw new RuntimeException("Нет верных ответов для вопроса: " + tokens[0]);
        }

        if (tokens.length > 5) {
            entity.setPhotoFilename(QuizUtils.generateFilename(tokens[5]));
            entity.setOriginalPhotoUrl(tokens[5]);
        }
        entity.setCreatedBy(QuizUtils.getLoggedUser());
        entity.setCreationDate(LocalDateTime.now());
        entity.setType(correctAnswersCount > 1 ? QuestionType.MULTI : QuestionType.TEXT);
        entity.setCategory(questionService.getOrCreateDefaultCategory());

        return entity;
    }

    private Pair<String, Boolean> parseToken(String tokenValue) {
        boolean correct = tokenValue.startsWith("_");
        String val = correct ? tokenValue.substring(1) : tokenValue;
        return Pair.of(val, correct);
    }
}
