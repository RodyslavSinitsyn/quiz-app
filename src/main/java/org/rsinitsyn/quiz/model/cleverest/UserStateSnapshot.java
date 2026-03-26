package org.rsinitsyn.quiz.model.cleverest;

public record UserStateSnapshot(String username,
                                String color,
                                byte[] avatar,
                                String answerText,
                                boolean correct,
                                boolean answerGiven,
                                long lastResponseTimeMs,
                                int score) {
}
