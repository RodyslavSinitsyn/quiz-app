package org.rsinitsyn.quiz.model;

import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public record HintsState(boolean hintsEnabled, Map<AnswerHint, Boolean> hintsUsage) {

    public static HintsState disabledHintsState() {
        return new HintsState(false,
                stream(AnswerHint.values()).collect(toMap(identity(), hint -> false)));
    }
}
