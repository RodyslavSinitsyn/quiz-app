package org.rsinitsyn.quiz.config;

import org.rsinitsyn.quiz.model.binding.AbstractQuestionBindingModel;
import org.rsinitsyn.quiz.service.strategy.update.AbstractQuestionUpdateStrategy;
import org.rsinitsyn.quiz.service.strategy.update.QuestionUpdateStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class CustomBeans {

    @Bean
    @Qualifier("questionUpdateStrategyMap")
    public Map<String, AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel>> questionUpdateStrategyMap(
            List<AbstractQuestionUpdateStrategy<? extends AbstractQuestionBindingModel>> strategyList) {
        return strategyList.stream().collect(Collectors.toMap(
                QuestionUpdateStrategy::getBindingModelClassName,
                Function.identity()
        ));
    }
}
