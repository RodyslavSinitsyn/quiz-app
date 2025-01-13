package org.rsinitsyn.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class HintsState {
    private boolean hintsEnabled;
    private Map<AnswerHint, Boolean> hintsUsage = new HashMap<>();

    public static HintsState disabled() {
        var hintsState = new HintsState();
        hintsState.hintsEnabled = false;
        hintsState.hintsUsage = Arrays.stream(AnswerHint.values())
                .collect(Collectors.toMap(Function.identity(), hint -> false));
        return hintsState;
    }
}
