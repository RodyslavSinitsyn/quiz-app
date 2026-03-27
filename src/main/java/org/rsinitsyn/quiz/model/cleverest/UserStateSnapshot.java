package org.rsinitsyn.quiz.model.cleverest;

public record UserStateSnapshot(UserProfile profile,
                                String answerText,
                                boolean correct,
                                boolean answerGiven,
                                long lastResponseTimeMs,
                                int score) {

    public String username() {
        return profile.username();
    }

    public String color() {
        return profile.color();
    }
}
