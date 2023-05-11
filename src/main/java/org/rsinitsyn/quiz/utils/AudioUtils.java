package org.rsinitsyn.quiz.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

@UtilityClass
@Slf4j
public class AudioUtils {

    private static final String AUDIO_FILES_FOLDER = "audio/";
    private static final String STATIC_FILES_FOLDER = "static/";

    public CompletableFuture<Void> playSoundAsync(String audioFileName) {
        return CompletableFuture.runAsync(() -> createPlayerAndPlay(audioFileName));
    }

    public CompletableFuture<Void> playStaticSoundAsync(String audioFileName) {
        return CompletableFuture.runAsync(() -> createPlayerAndPlay(STATIC_FILES_FOLDER + audioFileName));
    }

    public Player playStaticAudioAsyncAndGetPlayer(String audioFileName) {
        var audioData = createPlayerAndBuffer(STATIC_FILES_FOLDER + audioFileName);
        CompletableFuture.runAsync(() -> {
            try {
                audioData.getFirst().play();
            } catch (JavaLayerException e) {
                log.error("Error when play audio", e);
            } finally {
                log.info("Close audio buffer");
                closeBuffer(audioData.getSecond());
            }
        });
        return audioData.getFirst();
    }

    @SneakyThrows
    private void createPlayerAndPlay(String audioFileName) {
        Pair<Player, BufferedInputStream> data = createPlayerAndBuffer(audioFileName);
        try {
            data.getFirst().play();
        } finally {
            closeBuffer(data.getSecond());
        }
    }

    private Pair<Player, BufferedInputStream> createPlayerAndBuffer(String audioFileName) {
        final String path = AUDIO_FILES_FOLDER + audioFileName;
        try {
            BufferedInputStream buffer = new BufferedInputStream(
                    new FileInputStream(QuizUtils.readFileFromResources(path)));
            Player player = new Player(buffer);
            return Pair.of(player, buffer);
        } catch (IOException | JavaLayerException e) {
            throw new RuntimeException(e);
        }
    }

    private void closeBuffer(BufferedInputStream bufferedInputStream) {
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            log.error("Error when closing audio buffer", e);
        }
    }
}
