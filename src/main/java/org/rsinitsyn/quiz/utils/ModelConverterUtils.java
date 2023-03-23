package org.rsinitsyn.quiz.utils;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionModel;
import org.rsinitsyn.quiz.model.ViktorinaQuestion;

@UtilityClass
public class ModelConverterUtils {

    public List<ViktorinaQuestion> toViktorinaQuestions(Collection<FourAnswersQuestionModel> fourAnswersQuestionModels) {
        return fourAnswersQuestionModels.stream().map(ModelConverterUtils::toViktorinaQuestion).collect(Collectors.toList());
    }

    public ViktorinaQuestion toViktorinaQuestion(FourAnswersQuestionModel fourAnswersQuestionModel) {
        return new ViktorinaQuestion(fourAnswersQuestionModel.getText(),
                Collections.singleton(new ViktorinaQuestion.ViktorinaAnswer(fourAnswersQuestionModel.getCorrectAnswerText(), true)));
    }

    public List<FourAnswersQuestionModel> toQuestionModels(Collection<QuestionEntity> questionEntities) {
        return questionEntities.stream().map(ModelConverterUtils::toQuestionModel).collect(Collectors.toList());
    }

    public FourAnswersQuestionModel toQuestionModel(QuestionEntity questionEntity) {
        List<AnswerEntity> answers = questionEntity.getAnswers().stream()
                        .sorted(Comparator.comparing(AnswerEntity::isCorrect, Comparator.reverseOrder()))
                .toList();
        return new FourAnswersQuestionModel(
                questionEntity.getId(),
                questionEntity.getText(),
                answers.get(0).getAnswerText(),
                answers.get(1).getAnswerText(),
                answers.get(2).getAnswerText(),
                answers.get(3).getAnswerText());
    }

    public QuestionEntity toQuestionEntity(FourAnswersQuestionModel fourAnswersQuestionModel) {
        QuestionEntity questionEntity = new QuestionEntity();
        questionEntity.setId(fourAnswersQuestionModel.getId());
        questionEntity.setText(fourAnswersQuestionModel.getText());

        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionModel.getCorrectAnswerText(), true));
        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionModel.getSecondOptionAnswerText(), false));
        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionModel.getThirdOptionAnswerText(), false));
        questionEntity.addAnswer(createAnswerEntity(fourAnswersQuestionModel.getFourthOptionAnswerText(), false));

        return questionEntity;
    }

    private AnswerEntity createAnswerEntity(String text, boolean correct) {
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setAnswerText(text);
        answerEntity.setCorrect(correct);
        return answerEntity;
    }
}
