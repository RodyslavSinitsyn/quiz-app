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

    public List<QuizQuestionModel> toViktorinaQuestions(Collection<FourAnswersQuestionBindingModel> fourAnswersQuestionBindingModels) {
        return fourAnswersQuestionBindingModels.stream().map(ModelConverterUtils::toViktorinaQuestion).collect(Collectors.toList());
    }

    public QuizQuestionModel toViktorinaQuestion(FourAnswersQuestionBindingModel model) {
        return new QuizQuestionModel(model.getText(),
                Set.of(new QuizQuestionModel.QuizAnswerModel(model.getCorrectAnswerText(), true),
                        new QuizQuestionModel.QuizAnswerModel(model.getSecondOptionAnswerText(), false),
                        new QuizQuestionModel.QuizAnswerModel(model.getThirdOptionAnswerText(), false),
                        new QuizQuestionModel.QuizAnswerModel(model.getFourthOptionAnswerText(), false)));
    }

    public List<FourAnswersQuestionBindingModel> toQuestionModels(Collection<QuestionEntity> questionEntities) {
        return questionEntities.stream().map(ModelConverterUtils::toQuestionModel).collect(Collectors.toList());
    }

    public FourAnswersQuestionBindingModel toQuestionModel(QuestionEntity questionEntity) {
        List<AnswerEntity> answers = questionEntity.getAnswers().stream()
                .sorted(Comparator.comparing(AnswerEntity::isCorrect, Comparator.reverseOrder()))
                .toList();
        return new FourAnswersQuestionBindingModel(
                questionEntity.getId(),
                questionEntity.getText(),
                answers.get(0).getAnswerText(),
                answers.get(1).getAnswerText(),
                answers.get(2).getAnswerText(),
                answers.get(3).getAnswerText());
    }

    public QuestionEntity toQuestionEntity(FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setId(fourAnswersQuestionBindingModel.getId());
        questionEntity.setText(fourAnswersQuestionBindingModel.getText());

        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionBindingModel.getCorrectAnswerText(), true));
        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionBindingModel.getSecondOptionAnswerText(), false));
        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionBindingModel.getThirdOptionAnswerText(), false));
        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionBindingModel.getFourthOptionAnswerText(), false));

        return questionEntity;
    }

    private AnswerEntity createAnswerEntity(String text, boolean correct) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setAnswerText(text);
        answerEntity.setCorrect(correct);
        return answerEntity;
    }
}
