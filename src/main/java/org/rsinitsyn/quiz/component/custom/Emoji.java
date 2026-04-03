package org.rsinitsyn.quiz.component.custom;

import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RequiredArgsConstructor
public enum Emoji {

    SKULL("💀", 1),
    SHIT("💩", 1),
    DEVIL("👿", 2),

    POKER_FACE("😐", 3),
    SLEEPY("🥱", 3),
    GOOD("😊", 4),

    PARTY("🥳", 4),
    LOVE("😍", 5),
    AMAZING("🤩", 5),

    SOUND("🔊"),
    CHAT("💬"),
    GUITAR("🎸"),
    LAUGH("😂"),
    EXPLODE("🤯", 2),

    HEART("❤️", 5),
    FIRE("🔥", 5),
    CLAP("👏"),
    SHOCK("😱"),

    ROFL("🤣"),
    CRY("😭", 2),
    ANGRY("😡", 1),
    MIND_BLOWN("🫨"),
    SUS("🧐", 3),

    MONKEY("🙈", 3),
    ALIEN("👽"),
    GHOST("👻", 2),
    CLOWN("🤡", 1),
    MELT("🫠", 2),

    SALUTE("🫡"),
    EYES("👀"),
    BRAIN("🧠", 4),
    MONEY("🤑"),
    NERD("🤓", 3),

    RAGE("😤"),
    COOL("😎", 4),
    YAWN("🥴", 3),
    CONFUSED("😵", 2),

    BANANA("🍌"),
    BURGER("🍔"),
    BEER("🍺"),
    ROCKET("🚀", 5);

    public final String value;
    public final int rating;

    Emoji(final String value) {
        this.value = value;
        this.rating = -1;
    }

    private static final List<Integer> BAD_RATINGS = List.of(1, 2);
    private static final List<Integer> MID_RATINGS = List.of(3);
    private static final List<Integer> GOOD_RATINGS = List.of(4, 5);

    public static final List<Emoji> BAD_LIST;
    public static final List<Emoji> MID_LIST;
    public static final List<Emoji> GOOD_LIST;
    public static final List<Emoji> REACTION_LIST = List.of(HEART, LAUGH, FIRE, CLAP, SHOCK);

    static {
        BAD_LIST = Arrays.stream(values()).filter(e -> e.rating > 0).filter(e -> BAD_RATINGS.contains(e.rating)).toList();
        MID_LIST = Arrays.stream(values()).filter(e -> e.rating > 0).filter(e -> MID_RATINGS.contains(e.rating)).toList();
        GOOD_LIST = Arrays.stream(values()).filter(e -> e.rating > 0).filter(e -> GOOD_RATINGS.contains(e.rating)).toList();
    }

    private static Emoji random(List<Emoji> emojis) {
        return emojis.get(ThreadLocalRandom.current().nextInt(emojis.size()));
    }

    public static Emoji randomBad() {
        return random(BAD_LIST);
    }

    public static Emoji randomMid() {
        return random(MID_LIST);
    }

    public static Emoji randomGood() {
        return random(GOOD_LIST);
    }

    public static Emoji randomReaction() {
        return random(REACTION_LIST);
    }
}
