package org.rsinitsyn.quiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class QuizAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizAppApplication.class, args);
    }

}
