package org.rsinitsyn.quiz.component.custom;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public enum Emoji {

    SKULL("💀", 1),
    SHIT("💩", 1),
    DEVIL("👿", 2),

    POKER_FACE("😐", 2),
    SLEEPY("🥱", 3),
    GOOD("😊", 4),

    PARTY("🥳", 4),
    LOVE("😍", 5),
    AMAZING("🤩", 5),

    SOUND("🔊"),
    CHAT("💬"),
    GUITAR("🎸"),
    LAUGH("😂"),
    EXPLODE("🤯");

    public final String value;
    public final int rating;

    Emoji(final String value) {
        this.value = value;
        this.rating = -1;
    }

    public static final List<Emoji> BAD_LIST = List.of(SKULL, DEVIL, SHIT);
    public static final List<Emoji> GOOD_LIST = List.of(POKER_FACE, SLEEPY, GOOD);
    public static final List<Emoji> GREAT_LIST = List.of(PARTY, LOVE, AMAZING);

    private static Emoji random(List<Emoji> emojis) {
        return emojis.get(ThreadLocalRandom.current().nextInt(emojis.size()));
    }

    public static Emoji randomBad() {
        return random(BAD_LIST);
    }

    public static Emoji randomGood() {
        return random(GOOD_LIST);
    }

    public static Emoji randomGreat() {
        return random(GREAT_LIST);
    }
}
