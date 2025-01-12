package org.rsinitsyn.quiz;

import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.lang.management.ManagementFactory;
import java.util.List;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableConfigurationProperties(value = QuizAppProperties.class)
@EnableCaching
@EnableScheduling
@Slf4j
public class QuizAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizAppApplication.class, args);
        var arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        log.info("Application started with arguments: {}", arguments);
    }

}
