package org.rsinitsyn.quiz.model.cleverest;

import java.util.Optional;
import java.util.UUID;

public record UserStateSnapshot(UserProfile profile,
                                String answerText,
                                boolean correct,
                                boolean answerGiven,
                                long lastResponseTimeMs,
                                int score,
                                int position,
                                Optional<UUID> questionId) {

    public String username() {
        return profile.username();
    }

    public String color() {
        return profile.color();
    }
}
