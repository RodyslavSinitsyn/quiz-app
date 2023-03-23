package org.rsinitsyn.quiz.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

public class AudioService {

    private static final String AUDIO_FILES_FOLDER = "audio/";

    public void playSound(String audioFileName) {
        CompletableFuture.runAsync(() -> playSoundAsync(audioFileName));
    }

    public void playSoundAsync(String audioFileName) {
        final String path = AUDIO_FILES_FOLDER + audioFileName;
        try (BufferedInputStream buffer = new BufferedInputStream(
                getClass().getClassLoader().getResourceAsStream(path))) {
            Player player = new Player(buffer);
            player.play();
        } catch (IOException | JavaLayerException e) {
            e.printStackTrace();
        }
    }
}