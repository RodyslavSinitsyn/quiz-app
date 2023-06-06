package org.rsinitsyn.quiz.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.OrQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.TopQuestionBindingModel;

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
                questionEntity.getCategory().getName(),
                questionEntity.getCreatedBy(),
                questionEntity.getOriginalPhotoUrl(),
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
        model.setAnswerText(Double.valueOf(questionEntity.getAnswers().stream().findFirst().orElseThrow().getText()));
        model.setPhotoLocation(questionEntity.getOriginalPhotoUrl());
        model.setAnswerDescriptionText(questionEntity.getAnswerDescriptionText());

        return model;
    }

    public static OrQuestionBindingModel toOrQuestionBindingModel(QuestionEntity questionEntity) {
        return new OrQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                new ArrayList<>(questionEntity.getAnswers()).get(0).getText(),
                new ArrayList<>(questionEntity.getAnswers()).get(1).getText(),
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getAnswerDescriptionText()
        );
    }

    public static TopQuestionBindingModel toTopQuestionBindingModel(QuestionEntity questionEntity) {
        return new TopQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getAnswers().stream().map(AnswerEntity::getText).collect(Collectors.joining(System.lineSeparator())),
                questionEntity.getOriginalPhotoUrl(),
                questionEntity.getAnswerDescriptionText()
        );
    }
}
