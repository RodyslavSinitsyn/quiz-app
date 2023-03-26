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
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImportService {

    private final QuestionService questionService;

    @SneakyThrows
    public void importQuestions(InputStream fileContent) {
        String data = IOUtils.toString(fileContent, StandardCharsets.UTF_8);
        IOUtils.close(fileContent);

        String[] lines = data.split("\n");
        List<QuestionEntity> questionEntities = Arrays.stream(lines).map(this::toEntity).toList();
        questionService.saveAll(questionEntities);
    }

    private QuestionEntity toEntity(String line) {
        String[] tokens = line.split("[|]");

        QuestionEntity entity = new QuestionEntity();
        entity.setText(tokens[0]);

        entity.addAnswer(new AnswerEntity(tokens[1], true));
        entity.addAnswer(new AnswerEntity(tokens[2], false));
        entity.addAnswer(new AnswerEntity(tokens[3], false));
        entity.addAnswer(new AnswerEntity(tokens[4], false));

        entity.setPhotoFilename(null);
        entity.setCreatedBy(QuizUtils.getLoggedUser());
        entity.setCreationDate(LocalDateTime.now());
        entity.setType(QuestionType.TEXT);
        entity.setCategory(questionService.getOrCreateDefaultCategory());

        return entity;
    }
}
