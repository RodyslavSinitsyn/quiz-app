package org.rsinitsyn.quiz.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@UtilityClass
public class ModelConverterUtils {

    public List<QuizQuestionModel> toQuizQuestionModels(Collection<QuestionEntity> questionEntities) {
        return questionEntities.stream().map(ModelConverterUtils::toQuizQuestionModel).toList();
    }

    public QuizQuestionModel toQuizQuestionModel(QuestionEntity entity) {
        return new QuizQuestionModel(entity.getText(),
                entity.getType(),
                entity.getPhotoFilename(),
                entity.getCategory().getName(),
                toQuizQueestionAnswerModels(entity.getAnswers()));
    }

    private Set<QuizQuestionModel.QuizAnswerModel> toQuizQueestionAnswerModels(Set<AnswerEntity> answers) {
        return answers.stream()
                .map(answerEntity -> new QuizQuestionModel.QuizAnswerModel(
                        answerEntity.getText(),
                        answerEntity.isCorrect()))
                .collect(Collectors.toSet());
    }


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
