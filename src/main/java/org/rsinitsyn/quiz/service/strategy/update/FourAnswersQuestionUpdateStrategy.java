package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Service
public class FourAnswersQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<FourAnswersQuestionBindingModel> {


    public FourAnswersQuestionUpdateStrategy(QuizAppProperties properties,
                                             QuestionCategoryService categoryService) {
        super(properties, categoryService);
    }

    @Override
    public void setType(FourAnswersQuestionBindingModel model, QuestionEntity question) {
        if (model.hasMultiCorrectOptions()) {
            question.setType(QuestionType.MULTI);
        } else {
            question.setType(QuestionType.TEXT);
        }
    }

    @Override
    protected void createHook(FourAnswersQuestionBindingModel model, QuestionEntity question) {
        super.createHook(model, question);
        AtomicInteger counter = new AtomicInteger(0);
        model.getAnswers().forEach(answerBindingModel -> {
            question.addAnswer(createAnswerEntity(answerBindingModel.getText(),
                    answerBindingModel.isCorrect(),
                    counter.getAndIncrement(),
                    null));
        });
    }

    @Override
    protected void updateHook(FourAnswersQuestionBindingModel model, QuestionEntity question, QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        question.setCreatedBy(model.getAuthor());
        updateAnswerEntities(question, model.getAnswers());
    }

    @Override
    public String getBindingModelClassName() {
        return FourAnswersQuestionBindingModel.class.getSimpleName();
    }

    private void updateAnswerEntities(
            QuestionEntity entity,
            List<FourAnswersQuestionBindingModel.AnswerBindingModel> answerModels) {
        Function<UUID, FourAnswersQuestionBindingModel.AnswerBindingModel> getById = id ->
                answerModels
                        .stream()
                        .filter(am -> am.getId().equals(id))
                        .findFirst().orElseThrow();
        entity.getAnswers().forEach(answerEntity -> {
            final var answerModel = getById.apply(answerEntity.getId());
            answerEntity.setText(answerModel.getText());
            answerEntity.setNumber(answerModel.getIndex());
            answerEntity.setCorrect(answerModel.isCorrect());
        });
    }
}
