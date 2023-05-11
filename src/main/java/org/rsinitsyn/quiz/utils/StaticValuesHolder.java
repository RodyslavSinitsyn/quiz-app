package org.rsinitsyn.quiz.utils;

import com.google.common.collect.Iterables;
import java.util.Iterator;

public class StaticValuesHolder {

    public static final Iterator<String> SUBMIT_ANSWER_SHORT_AUDIOS = Iterables.cycle(
                    "submit-answer-short-1.mp3",
                    "submit-answer-short-2.mp3",
                    "submit-answer-short-3.mp3")
            .iterator();

    public static final Iterator<String> REVEAL_ANSWER_AUDIOS = Iterables.cycle(
            "reveal-answer-1.mp3", "reveal-answer-2.mp3").iterator();

    public static final Iterator<String> SUBMIT_ANSWER_AUDIOS = Iterables.cycle(
                    "submit-answer-1.mp3",
                    "submit-answer-2.mp3",
                    "submit-answer-3.mp3")
            .iterator();

    public static final Iterator<String> CORRECT_ANSWER_AUDIOS = Iterables.cycle(
                    "correct-answer-1.mp3",
                    "correct-answer-2.mp3",
                    "correct-answer-3.mp3",
                    "correct-answer-4.mp3",
                    "correct-answer-5.mp3",
                    "correct-answer-6.mp3")
            .iterator();

    public static final Iterator<String> WRONG_ANSWER_AUDIOS = Iterables.cycle(
                    "wrong-answer-1.mp3",
                    "wrong-answer-2.mp3",
                    "wrong-answer-3.mp3")
            .iterator();

    public static final Iterator<String> THINK_AUDIOS = Iterables.cycle(
                    "think-1.mp3",
                    "think-2.mp3",
                    "think-3.mp3")
            .iterator();

    public static final String BLACK_FONT_BORDER = "2px 2px 2px black";
}
