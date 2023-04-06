package org.rsinitsyn.quiz.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AudioUtils {

    private static final String AUDIO_FILES_FOLDER = "audio/";
    private static final String STATIC_FILES_FOLDER = "static/";

    public CompletableFuture<Void> playSoundAsync(String audioFileName) {
        return CompletableFuture.runAsync(() -> playSound(audioFileName));
    }

    public CompletableFuture<Void> playStaticSoundAsync(String audioFileName) {
        return CompletableFuture.runAsync(() -> playSound(STATIC_FILES_FOLDER + audioFileName));
    }

    // todo NEED TO REALOD APP AFTER ADDING CUSTOM AUDIO TODO FIX LATER
    public void playSound(String audioFileName) {
        final String path = AUDIO_FILES_FOLDER + audioFileName;
        try (BufferedInputStream buffer = new BufferedInputStream(
                new FileInputStream(QuizUtils.readFileFromResources(path)))) {
            Player player = new Player(buffer);
            player.play();
        } catch (IOException | JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }
}
