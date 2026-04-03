package org.rsinitsyn.quiz.model.cleverest;

import java.util.function.Consumer;

public record ManualApprove(int clickLimit, int pointsPerClick, Consumer<String> action) {
}
