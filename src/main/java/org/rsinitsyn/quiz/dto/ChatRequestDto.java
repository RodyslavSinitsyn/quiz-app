package org.rsinitsyn.quiz.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ChatRequestDto {

    private String model;
    private List<Message> messages;
    private int n = 1;
    private double temperature;

    public ChatRequestDto(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static class Message {
        private String role;
        private String content;
    }
}