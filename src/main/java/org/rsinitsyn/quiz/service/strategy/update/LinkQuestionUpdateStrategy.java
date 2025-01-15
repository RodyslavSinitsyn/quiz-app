package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.LinkQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LinkQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<LinkQuestionBindingModel> {


    public LinkQuestionUpdateStrategy(QuizAppProperties properties,
                                      QuestionCategoryService categoryService) {
        super(properties, categoryService);
    }

    @Override
    public void setType(LinkQuestionBindingModel model, QuestionEntity question) {
        question.setType(QuestionType.LINK);
    }

    @Override
    protected void createHook(LinkQuestionBindingModel model, QuestionEntity question) {
        super.createHook(model, question);
        appendAnswers(model, question);
    }

    @Override
    protected void updateHook(LinkQuestionBindingModel model, QuestionEntity question, QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        question.getAnswers().clear();
        appendAnswers(model, question);
    }

    private void appendAnswers(LinkQuestionBindingModel model, QuestionEntity question) {
        AtomicInteger counter = new AtomicInteger(0);
        model.getLeftAnswers().lines().forEach(
                line -> question.addAnswer(createAnswerEntity(line, true, counter.getAndIncrement(), null)));

        counter.set(0);
        model.getRightAnswers().lines().forEach(
                line -> question.addAnswer(createAnswerEntity(line, false, counter.getAndIncrement(), null)));
    }

    @Override
    public String getBindingModelClassName() {
        return LinkQuestionBindingModel.class.getSimpleName();
    }
}
