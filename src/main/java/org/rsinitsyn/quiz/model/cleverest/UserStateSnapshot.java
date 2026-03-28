package org.rsinitsyn.quiz.model.cleverest;

import org.rsinitsyn.quiz.entity.AnswerStatus;

import java.util.Optional;
import java.util.UUID;

import static org.rsinitsyn.quiz.entity.AnswerStatus.CORRECT;

public record UserStateSnapshot(UserProfile profile,
                                String answerText,
                                AnswerStatus answerStatus,
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

    public boolean correct() {
        return answerStatus == CORRECT;
    }
}
