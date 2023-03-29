package org.rsinitsyn.quiz.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;

@UtilityClass
public class ModelConverterUtils {

    public List<FourAnswersQuestionBindingModel> toFourAnswersQuestionBindingModels(Collection<QuestionEntity> questionEntities) {
        return questionEntities.stream().map(ModelConverterUtils::toFourAnswersQuestionBindingModel).collect(Collectors.toList());
    }

    public FourAnswersQuestionBindingModel toFourAnswersQuestionBindingModel(QuestionEntity questionEntity) {
        if (questionEntity == null) {
            return new FourAnswersQuestionBindingModel();
        }
        List<AnswerEntity> answers = questionEntity.getAnswers().stream()
                .sorted(Comparator.comparing(AnswerEntity::isCorrect, Comparator.reverseOrder()))
                .toList();
        return new FourAnswersQuestionBindingModel(
                questionEntity.getId().toString(),
                questionEntity.getText(),
                questionEntity.getCategory().getName(),
                questionEntity.getCreatedBy(),
                answers.get(0).getText(),
                answers.get(1).getText(),
                answers.get(2).getText(),
                answers.get(3).getText(),
                questionEntity.getOriginalPhotoUrl());
    }
}
