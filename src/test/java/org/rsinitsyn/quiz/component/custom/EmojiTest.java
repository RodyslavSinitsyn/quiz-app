package org.rsinitsyn.quiz.component.custom;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class EmojiTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void test(Emoji emoji, Predicate<Integer> test) {
        assertThat(test.test(emoji.rating)).isTrue();
    }

    private static Stream<Arguments> test() {
        return Stream.of(
                arguments(Emoji.randomBad(), (Predicate<Integer>) r -> r > 0 && r < 3),
                arguments(Emoji.randomMid(), (Predicate<Integer>) r -> r == 3),
                arguments(Emoji.randomGood(), (Predicate<Integer>) r -> r > 3 && r <= 5)
        );
    }
}