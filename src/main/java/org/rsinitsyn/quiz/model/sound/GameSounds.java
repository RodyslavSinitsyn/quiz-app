package org.rsinitsyn.quiz.model.sound;

import com.google.common.collect.Iterables;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class GameSounds {

    private static final List<GameSound> ALL = load();

    private static final Iterator<GameSound> CYCLE =
            Iterables.cycle(ALL).iterator();

    private static List<GameSound> load() {
        try (final var stream = Files.list(Path.of("src/main/resources/audio/static/sounds"))) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> new GameSound(
                            path.getFileName().toString(),
                            SoundCategory.FUNNY
                    ))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load sounds", e);
        }
    }

    public static GameSound next() {
        return CYCLE.next();
    }

    public static GameSound random() {
        return ALL.get(ThreadLocalRandom.current().nextInt(ALL.size()));
    }
}
