package org.rsinitsyn.quiz.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class AudioService {

    private static final String AUDIO_FILES_FOLDER = "audio/";

    public void playSound(String audioFileName) {
//        CompletableFuture.runAsync(() -> playSoundAsync(audioFileName));
        playSoundAsync(audioFileName);
    }

    public void playSoundAsync(String audioFileName) {
        final String path = AUDIO_FILES_FOLDER + audioFileName;
        try (BufferedInputStream buffer = new BufferedInputStream(
                getClass().getClassLoader().getResourceAsStream(path))) {
            Player mp3Player = new Player(buffer);
            mp3Player.play();
        } catch (IOException | JavaLayerException e) {
            e.printStackTrace();
        }
    }
}
