package org.rsinitsyn.quiz.model.cleverest;

import java.time.Instant;
import java.util.Optional;

public record UserMessage(String username,
                          String message,
                          Instant date,
                          Optional<String> photoUrl) {
}
