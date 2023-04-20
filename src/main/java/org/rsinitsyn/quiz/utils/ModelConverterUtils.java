package org.rsinitsyn.quiz.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;

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
                questionEntity.getOriginalPhotoUrl());
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

        return model;
    }
}
