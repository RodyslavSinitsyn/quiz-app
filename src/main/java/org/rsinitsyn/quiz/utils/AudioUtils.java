package org.rsinitsyn.quiz.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AudioUtils {

    private static final String AUDIO_FILES_FOLDER = "audio/";

    public CompletableFuture<Void> playSoundAsync(String audioFileName) {
        return CompletableFuture.runAsync(() -> playSound(audioFileName));
    }

    public void playSound(String audioFileName) {
        final String path = AUDIO_FILES_FOLDER + audioFileName;
        try (BufferedInputStream buffer = new BufferedInputStream(
                AudioUtils.class.getClassLoader().getResourceAsStream(path))) {
            Player player = new Player(buffer);
            player.play();
        } catch (IOException | JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }
}
