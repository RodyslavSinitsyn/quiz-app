package org.rsinitsyn.quiz.entity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.rsinitsyn.quiz.entity.AnswerStatus.*;

class AnswerStatusTest {

    @ParameterizedTest
    @MethodSource
    void answer_status_from_ints(int correct, int max, AnswerStatus expected) {
        assertThat(answerStatus(correct, max)).isEqualTo(expected);
    }

    private static Stream<Arguments> answer_status_from_ints() {
        return Stream.of(
                arguments(1, 1, CORRECT),
                arguments(0, 1, WRONG),
                arguments(1, 2, PARTIAL)
        );
    }
}