package org.rsinitsyn.quiz.model.cleverest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class RoundRevealPlanTest {

    @ParameterizedTest
    @MethodSource
    void test(int listSize, int revealsCount, int questionNumber, int leftToReveal, int lastN) {
        final var plan = RoundRevealPlan.of(listSize, revealsCount);

        assertSoftly(softly -> {
            softly.assertThat(plan.questionsUntilNextReveal(questionNumber - 1)).isEqualTo(leftToReveal);
            softly.assertThat(plan.getLastN(questionNumber - 1)).isEqualTo(lastN);
        });
    }


    public static Stream<Arguments> test() {
        return Stream.of(
                // size 1, no matter how many reveals
                arguments(1, 1, 1, 0, 1),
                arguments(1, 2, 1, 0, 1),
                arguments(1, 3, 1, 0, 1),

                // size 2, 1 reveal
                arguments(2, 1, 1, 1, -1),
                arguments(2, 1, 2, 0, 2),

                // size 2, 2 reveals
                arguments(2, 2, 1, 0, 1),
                arguments(2, 2, 2, 0, 1),

                // size 9, 3 reveals
                arguments(9, 3, 1, 2, -1),
                arguments(9, 3, 2, 1, -1),
                arguments(9, 3, 3, 0, 3),

                arguments(10, 3, 1, 3, -1),
                arguments(10, 3, 4, 0, 4),
                arguments(10, 3, 6, 1, -1),
                arguments(10, 3, 7, 0, 3),
                arguments(10, 3, 10, 0, 3),

                arguments(30, 3, 1, 9, -1),
                arguments(30, 3, 5, 5, -1),
                arguments(30, 3, 10, 0, 10),
                arguments(30, 3, 30, 0, 10)
        );
    }
}