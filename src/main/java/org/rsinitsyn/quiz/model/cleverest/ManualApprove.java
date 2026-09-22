package org.rsinitsyn.quiz.model.cleverest;

import java.util.function.Consumer;

public record ManualApprove(int maxPoints, Consumer<String> approve, Consumer<String> reject) {
}
