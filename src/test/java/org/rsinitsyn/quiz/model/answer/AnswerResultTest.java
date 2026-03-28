package org.rsinitsyn.quiz.model.answer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rsinitsyn.quiz.entity.AnswerStatus.CORRECT;
import static org.rsinitsyn.quiz.entity.AnswerStatus.WRONG;
import static org.rsinitsyn.quiz.model.answer.AnswerResult.oneOptionResult;

class AnswerResultTest {

    @ParameterizedTest
    @MethodSource
    void one_option_result(Boolean input, AnswerResult expected) {
        assertThat(oneOptionResult(input)).isEqualTo(expected);
    }

    public static Stream<Arguments> one_option_result() {
        return Stream.of(
                Arguments.of(true, new AnswerResult(CORRECT, 1, 1)),
                Arguments.of(false, new AnswerResult(WRONG, 1, 0))
        );
    }
}