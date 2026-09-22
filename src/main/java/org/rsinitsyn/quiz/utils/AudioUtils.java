package org.rsinitsyn.quiz.utils;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.rsinitsyn.quiz.utils.QuizUtils.readAudioFile;

@Slf4j
public final class AudioUtils {
    public static final String STATIC_FILES_FOLDER = "static/";

    private AudioUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static CompletableFuture<Void> playSoundAsync(String audioFileName) {
        return runAsync(() -> createAndPlayPlayer(audioFileName));
    }

    public static CompletableFuture<Void> playStaticSoundAsync(String audioFileName) {
        return runAsync(() -> createAndPlayPlayer(STATIC_FILES_FOLDER + audioFileName));
    }

    public static Player playStaticAudioAsyncAndGetPlayer(String audioFileName) {
        var audioData = createPlayerAndBuffer(STATIC_FILES_FOLDER + audioFileName);
        runAsync(() -> {
            try {
                audioData.getFirst().play();
            } catch (JavaLayerException e) {
                log.error("Error when play audio", e);
            } finally {
                log.debug("Close audio buffer");
                closeBuffer(audioData.getSecond());
            }
        });
        return audioData.getFirst();
    }

    @SneakyThrows
    private static void createAndPlayPlayer(String audioFileName) {
        Pair<Player, BufferedInputStream> data = createPlayerAndBuffer(audioFileName);
        try {
            data.getFirst().play();
        } finally {
            closeBuffer(data.getSecond());
        }
    }

    private static Pair<Player, BufferedInputStream> createPlayerAndBuffer(String pathToAudioFile) {
        try {
            BufferedInputStream buffer = new BufferedInputStream(
                    new FileInputStream(readAudioFile(pathToAudioFile)));
            Player player = new Player(buffer);
            return Pair.of(player, buffer);
        } catch (IOException | JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }

    private static void closeBuffer(BufferedInputStream bufferedInputStream) {
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            log.error("Error when closing audio buffer", e);
        }
    }
}
