package org.rsinitsyn.quiz.utils;

import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionHintEntity;
import org.rsinitsyn.quiz.model.binding.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.rsinitsyn.quiz.entity.QuestionType.SEQUENCE;

@UtilityClass
public class ModelConverterUtils {

    public List<FourAnswersQuestionBindingModel> toFourAnswersQuestionBindingModels(Collection<QuestionEntity> questionEntities) {
        return questionEntities.stream().map(ModelConverterUtils::toFourAnswersQuestionBindingModel).collect(Collectors.toList());
    }

    public FourAnswersQuestionBindingModel toFourAnswersQuestionBindingModel(QuestionEntity questionEntity) {
        if (questionEntity == null) {
            return new FourAnswersQuestionBindingModel();
        }

        List<FourAnswersQuestionBindingModel.AnswerBindingModel> answerBindingModels = new ArrayList<>();
        questionEntity.getAnswers()
                .stream()
                .map(answerEntity ->
                        new FourAnswersQuestionBindingModel.AnswerBindingModel(
                                answerEntity.getId(),
                                answerEntity.isCorrect(),
                                answerEntity.getText(),
                                answerEntity.getNumber()))
                .forEach(answerBindingModels::add);

        return new FourAnswersQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                answerBindingModels,
                questionEntity.getCreatedBy(),
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getCategory().getName(),
                questionEntity.getHintsAsText(),
                questionEntity.getAnswerDescriptionText());
    }

    public PrecisionQuestionBindingModel toPrecisionQuestionBindingModel(QuestionEntity questionEntity) {
        if (questionEntity == null) {
            return new PrecisionQuestionBindingModel();
        }

        PrecisionQuestionBindingModel model = new PrecisionQuestionBindingModel();
        model.setId(questionEntity.getId().toString());
        model.setText(questionEntity.getText());
        model.setRange(Double.valueOf(questionEntity.getValidRange()));
        model.setAnswerText(Double.valueOf(questionEntity.getCorrectAnswer().getText()));
        model.setPhotoLocation(questionEntity.getOriginalPhotoUrl());
        model.setCategory(questionEntity.getCategory().getName());
        model.setAnswerDescriptionText(questionEntity.getAnswerDescriptionText());

        return model;
    }

    public static OrQuestionBindingModel toOrQuestionBindingModel(QuestionEntity questionEntity) {
        return new OrQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getAnswers().get(0).getText(),
                questionEntity.getAnswers().get(1).getText(),
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getCategory().getName(),
                questionEntity.getAnswerDescriptionText(),
                questionEntity.getHintsAsText()
        );
    }

    public static TopQuestionBindingModel toTopQuestionBindingModel(QuestionEntity questionEntity) {
        return new TopQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getAnswers().stream().map(AnswerEntity::getText).collect(Collectors.joining(System.lineSeparator())),
                questionEntity.getType() == SEQUENCE,
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getCategory().getName(),
                questionEntity.getAnswerDescriptionText(),
                questionEntity.getHintsAsText()
        );
    }

    public static PhotoQuestionBindingModel toPhotoQuestionBindingModel(QuestionEntity questionEntity) {
//        List<AnswerEntity> answerEntities = questionEntity.getAnswers().stream().sorted(Comparator.comparing(AnswerEntity::getNumber)).toList();
        List<AnswerEntity> answerEntities = questionEntity.getAnswers();
        return new PhotoQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getAnswerDescriptionText(),
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getCategory().getName(),
                questionEntity.getHintsAsText(),
                answerEntities.get(0).getPhotoFilename(),
                answerEntities.get(1).getPhotoFilename(),
                answerEntities.get(2).getPhotoFilename(),
                answerEntities.get(3).getPhotoFilename()
        );
    }

    public static LinkQuestionBindingModel toLinkQuestionBindingModel(QuestionEntity questionEntity) {
        return new LinkQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getAnswerDescriptionText(),
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getCategory().getName(),
                questionEntity.getHintsAsText(),
                questionEntity.getAnswers().stream()
                        .filter(AnswerEntity::isCorrect)
                        .map(AnswerEntity::getText)
                        .collect(Collectors.joining(System.lineSeparator())),
                questionEntity.getAnswers().stream()
                        .filter(answerEntity -> !answerEntity.isCorrect())
                        .map(AnswerEntity::getText)
                        .collect(Collectors.joining(System.lineSeparator()))
        );
    }

    public static GuessPhotoQuestionBindingModel toGuessPhotoBindingModel(QuestionEntity questionEntity) {
        return new GuessPhotoQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getAnswerDescriptionText(),
                questionEntity.getPhotoFilename(),
                questionEntity.getCategory().getName(),
                questionEntity.getHintsAsText(),
                questionEntity.getHints().stream()
                        .filter(QuestionHintEntity::photoType)
                        .map(QuestionHintEntity::getPhotoFilename)
                        .toList(),
                questionEntity.getCorrectAnswer().getText()
        );
    }
}
