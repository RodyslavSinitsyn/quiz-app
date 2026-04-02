package org.rsinitsyn.quiz.model.cleverest;

import java.time.Instant;

public record UserMessage(String username,
                          String message,
                          Instant date) {
}
