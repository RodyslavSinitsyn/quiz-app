package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.OrQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.springframework.stereotype.Service;

@Service
public class OrQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<OrQuestionBindingModel> {


    public OrQuestionUpdateStrategy(QuizAppProperties properties,
                                    QuestionCategoryService categoryService) {
        super(properties, categoryService);
    }

    @Override
    public void setType(OrQuestionBindingModel model, QuestionEntity question) {
        question.setType(QuestionType.OR);
    }

    @Override
    protected void commonHook(OrQuestionBindingModel model, QuestionEntity question, QuestionEntity persistentEntity) {
        super.commonHook(model, question, persistentEntity);
        question.setOptionsOnly(true);
    }

    @Override
    protected void createHook(OrQuestionBindingModel model, QuestionEntity question) {
        super.createHook(model, question);
        var correctAnswer = createAnswerEntity(model.getCorrectAnswerText(),
                true,
                0,
                null);
        var optionAnswer = createAnswerEntity(model.getOptionAnswerText(),
                false,
                1,
                null);
        question.addAnswer(correctAnswer);
        question.addAnswer(optionAnswer);
    }

    @Override
    protected void updateHook(OrQuestionBindingModel model, QuestionEntity question, QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        var correct = question.getAnswers().stream().filter(AnswerEntity::isCorrect).findFirst().orElseThrow();
        correct.setText(model.getCorrectAnswerText());
        var option = question.getAnswers().stream().filter(a -> !a.isCorrect()).findFirst().orElseThrow();
        option.setText(model.getOptionAnswerText());
    }

    @Override
    public String getBindingModelClassName() {
        return OrQuestionBindingModel.class.getSimpleName();
    }
}
