package org.rsinitsyn.quiz.model;

public record UserStateSnapshot(String username,
                                String color,
                                String answerText,
                                boolean correct,
                                boolean answerGiven,
                                long lastResponseTimeMs,
                                int score) {
}
