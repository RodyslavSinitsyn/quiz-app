package org.rsinitsyn.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.rsinitsyn.quiz.entity.QuestionType.*;

@RequiredArgsConstructor
@Slf4j
@Service
public class JsonImportService {

    private static final Map<QuestionType, Class<? extends AbstractQuestionBindingModel>> BINDING_MODEL_TYPES =
            Map.of(
                    TEXT, FourAnswersQuestionBindingModel.class,
                    MULTI, FourAnswersQuestionBindingModel.class,
                    OR, OrQuestionBindingModel.class,
                    PHOTO, PhotoQuestionBindingModel.class,
                    LINK, LinkQuestionBindingModel.class,
                    TOP, TopQuestionBindingModel.class,
                    SEQUENCE, TopQuestionBindingModel.class,
                    PRECISION, PrecisionQuestionBindingModel.class,
                    GUESS_PHOTO, GuessPhotoQuestionBindingModel.class
            );

    private final ObjectMapper objectMapper;
    private final QuestionService questionService;

    @SneakyThrows
    @Transactional
    public void importQuestions(InputStream fileContent, final boolean dryRun) {
        final var nodes = objectMapper.readTree(fileContent);

        if (!nodes.isArray()) {
            throw new IllegalArgumentException("Expected JSON array");
        }

        StreamSupport.stream(nodes.spliterator(), false)
                .map(this::toBindingModel)
                .forEach(m -> execute(m, dryRun));
    }

    @SneakyThrows
    private AbstractQuestionBindingModel toBindingModel(JsonNode node) {
        final var type = node.path("type").asText();
        final Class<? extends AbstractQuestionBindingModel> aClass = BINDING_MODEL_TYPES.get(valueOf(type.toUpperCase()));
        if (aClass == null) {
            throw new IllegalArgumentException("Unsupported question type: %s".formatted(type));
        }
        return objectMapper.convertValue(node, aClass);
    }

    private void execute(AbstractQuestionBindingModel abstractQuestionBindingModel,
                         boolean dryRun) {
        final var questionEntity = questionService.prepareQuestionEntity(abstractQuestionBindingModel);
        if (dryRun) {
            log.debug("Dry run import. Creating question: {}", questionEntity);
        } else {
            questionService.saveEntityAndResources(questionEntity);
        }
    }
}
