package org.rsinitsyn.quiz.model.sound;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameSoundsTest {

    @Test
    void next() {
        final var next = GameSounds.next();
        assertThat(next).isNotNull();
    }
}