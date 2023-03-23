package org.rsinitsyn.quiz.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@UtilityClass
public class ModelConverterUtils {

    public List<QuizQuestionModel> toQuizQuestionModels(Collection<QuestionEntity> questionEntities) {
        return questionEntities.stream().map(ModelConverterUtils::toQuizQuestionModel).collect(Collectors.toList());
    }

    public QuizQuestionModel toQuizQuestionModel(QuestionEntity entity) {
        return new QuizQuestionModel(entity.getText(),
                entity.getType(),
                entity.getPhotoFilename(),
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
        List<AnswerEntity> answers = questionEntity.getAnswers().stream()
                .sorted(Comparator.comparing(AnswerEntity::isCorrect, Comparator.reverseOrder()))
                .toList();
        return new FourAnswersQuestionBindingModel(
                questionEntity.getId(),
                questionEntity.getText(),
                answers.get(0).getText(),
                answers.get(1).getText(),
                answers.get(2).getText(),
                answers.get(3).getText(),
                questionEntity.getPhotoFilename());
    }

    public QuestionEntity toQuestionEntity(FourAnswersQuestionBindingModel model) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setId(model.getId());
        questionEntity.setText(model.getText());

        questionEntity.addAnswer(createAnswerEntity(model.getCorrectAnswerText(), true));
        questionEntity.addAnswer(createAnswerEntity(model.getSecondOptionAnswerText(), false));
        questionEntity.addAnswer(createAnswerEntity(model.getThirdOptionAnswerText(), false));
        questionEntity.addAnswer(createAnswerEntity(model.getFourthOptionAnswerText(), false));

        if (model.getPhotoLocation() != null) {
            questionEntity.setType(QuestionType.PHOTO);
            questionEntity.setPhotoFilename(model.getPhotoLocation());
        } else {
            questionEntity.setType(QuestionType.TEXT);
        }

        return questionEntity;
    }

    private AnswerEntity createAnswerEntity(String text, boolean correct) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setText(text);
        answerEntity.setCorrect(correct);
        return answerEntity;
    }
}
