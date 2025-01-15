package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.TopQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class TopQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<TopQuestionBindingModel> {


    public TopQuestionUpdateStrategy(QuizAppProperties properties,
                                     QuestionCategoryService categoryService) {
        super(properties, categoryService);
    }

    @Override
    public void setType(TopQuestionBindingModel model, QuestionEntity question) {
        question.setType(QuestionType.TOP);
    }

    @Override
    protected void createHook(TopQuestionBindingModel model, QuestionEntity question) {
        super.createHook(model, question);
        appendAnswers(model, question);
    }

    @Override
    protected void updateHook(TopQuestionBindingModel model, QuestionEntity question, QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        question.getAnswers().clear();
        appendAnswers(model, question);
    }

    @Override
    public String getBindingModelClassName() {
        return TopQuestionBindingModel.class.getSimpleName();
    }

    private void appendAnswers(TopQuestionBindingModel model, QuestionEntity question) {
        AtomicInteger counter = new AtomicInteger(0);
        model.getTopListText().lines().forEach(
                answerText -> question.addAnswer(createAnswerEntity(answerText, true, counter.getAndIncrement(), null)));
    }
}
