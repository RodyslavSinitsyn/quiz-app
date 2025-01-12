package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.binding.AbstractQuestionBindingModel;

public interface QuestionUpdateStrategy<T extends AbstractQuestionBindingModel> {
    QuestionEntity prepareEntity(T bindingModel, QuestionEntity questionEntity);
    String getBindingModelClassName();
}
