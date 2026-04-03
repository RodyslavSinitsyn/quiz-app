package org.rsinitsyn.quiz.model.cleverest;

import java.util.ArrayList;
import java.util.List;

public record RoundRevealPlan(List<Integer> windows) {

    public static RoundRevealPlan of(int questionsSize, int revealsCount) {
        final var base = questionsSize / revealsCount;
        final var remainder = questionsSize % revealsCount;

        final var windows = new ArrayList<Integer>();
        var sum = 0;

        for (int i = 0; i < revealsCount; i++) {
            sum += i < remainder ? base + 1 : base;
            windows.add(sum);
        }

        return new RoundRevealPlan(windows);
    }

    public boolean shouldReveal(int currentQuestionNumber) {
        return windows.contains(currentQuestionNumber + 1);
    }

    public int questionsUntilNextReveal(int currentQuestionNumber) {
        final var current = currentQuestionNumber + 1;

        return windows.stream()
                .filter(window -> window >= current)
                .findFirst()
                .map(window -> window - current)
                .orElse(0);
    }

    public int getLastN(int currentQuestionNumber) {
        final var current = currentQuestionNumber + 1;

        final var revealIndex = windows.indexOf(current);

        if (revealIndex == -1) {
            return revealIndex;
//            throw new IllegalStateException("Current question is not reveal point");
        }

        if (revealIndex == 0) {
            return windows.getFirst();
        }

        return windows.get(revealIndex) - windows.get(revealIndex - 1);
    }
}
