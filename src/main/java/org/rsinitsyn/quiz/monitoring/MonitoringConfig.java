package org.rsinitsyn.quiz.monitoring;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MonitoringConfig {

    @Bean
    @ConditionalOnProperty(value = "quiz.monitoring", havingValue = "true")
    public ObservedAspect observedAspect(ObservationRegistry observationRegistry) {
        log.info("Creating ObservedAspect bean");
        observationRegistry.observationConfig().observationHandler(new SimpleLoggingHandler());
        return new ObservedAspect(observationRegistry);
    }
}
