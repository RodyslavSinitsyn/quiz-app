package org.rsinitsyn.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class HintsState {
    private boolean hintsEnabled;
    private Map<AnswerHint, Boolean> hintsUsage = new HashMap<>();
}
