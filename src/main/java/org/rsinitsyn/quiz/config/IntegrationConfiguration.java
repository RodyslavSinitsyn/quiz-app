package org.rsinitsyn.quiz.config;

import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class IntegrationConfiguration {

    @Bean
    @Qualifier("openaiRestTemplate")
    public RestTemplate openaiRestTemplate(@Autowired QuizAppProperties properties) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("Authorization", "Bearer " + properties.getOpenai().getApiKey());
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}
