package org.rsinitsyn.quiz;

import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.RestoreStateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static org.springframework.boot.SpringApplication.run;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableConfigurationProperties(value = QuizAppProperties.class)
@EnableCaching
@EnableScheduling
@Slf4j
public class QuizAppApplication {

    @Autowired
    private RestoreStateService restoreStateService;

    public static void main(String[] args) {
        run(QuizAppApplication.class, args);
    }

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        log.info("Application [{}] started with arguments: {}",
                event.getApplicationContext().getApplicationName(),
                getRuntimeMXBean().getInputArguments());
        restoreStateService.restore();
    }
}
