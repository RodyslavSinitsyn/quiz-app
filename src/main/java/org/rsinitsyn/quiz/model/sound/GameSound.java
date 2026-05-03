package org.rsinitsyn.quiz.model.sound;

public record GameSound(String path, SoundCategory category) {

    public String fullPath() {
        return "sounds/%s".formatted(path);
    }
}
