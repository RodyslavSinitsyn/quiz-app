package org.rsinitsyn.quiz.model.sound;

import com.google.common.collect.Iterables;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Arrays.stream;

public enum GameSound {

    KLICHKO_1("klichko_den.mp3", SoundCategory.FUNNY),
    YANIK_1("yanik_forget.mp3", SoundCategory.FUNNY),
    YANIK_2("yanik_standup.mp3", SoundCategory.FUNNY),
    DOBRYAK("dobryak.mp3", SoundCategory.FUNNY),
    DOBKIN("dobkin.mp3", SoundCategory.FUNNY);

    private final String path;
    public final SoundCategory category;

    GameSound(String path, SoundCategory category) {
        this.path = path;
        this.category = category;
    }

    public String path() {
        return "sounds/%s".formatted(path);
    }

    public static final Iterator<GameSound> CYCLE = Iterables.cycle(values()).iterator();

    public static GameSound random(SoundCategory category) {
        final var sounds = stream(values())
                .filter(sound -> sound.category == category)
                .toList();

        return sounds.get(ThreadLocalRandom.current().nextInt(sounds.size()));
    }

    public static GameSound next() {
        return CYCLE.next();
    }
}
