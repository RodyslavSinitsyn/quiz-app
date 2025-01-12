package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.dao.QuestionCategoryDao;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.springframework.stereotype.Service;

@Service
public class PreciseQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<PrecisionQuestionBindingModel> {

    public PreciseQuestionUpdateStrategy(QuizAppProperties properties, QuestionCategoryDao questionCategoryDao) {
        super(properties, questionCategoryDao);
    }

    @Override
    public void setType(PrecisionQuestionBindingModel model, QuestionEntity question) {
        question.setType(QuestionType.PRECISION);
    }

    @Override
    protected void createHook(PrecisionQuestionBindingModel model, QuestionEntity question) {
        super.createHook(model, question);
        question.setValidRange(model.getRange().intValue());
        var answerEntity = createAnswerEntity(String.valueOf(model.getAnswerText().intValue()),
                true,
                0,
                null);
        question.addAnswer(answerEntity);
    }

    @Override
    protected void updateHook(PrecisionQuestionBindingModel model, QuestionEntity question, QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        question.setValidRange(model.getRange().intValue());
        var answer = question.getAnswers().stream().findFirst().orElseThrow();
        answer.setText(model.getText());
    }

    @Override
    public String getBindingModelClassName() {
        return PrecisionQuestionBindingModel.class.getSimpleName();
    }
}
