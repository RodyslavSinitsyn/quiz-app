package org.rsinitsyn.quiz.model.sound;

import org.rsinitsyn.quiz.component.custom.Emoji;

public record GameSound(String path, Emoji emoji, SoundCategory category) {

    public String fullPath() {
        return "sounds/%s".formatted(path);
    }

}
