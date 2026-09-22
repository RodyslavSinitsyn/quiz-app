package org.rsinitsyn.quiz.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {
    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String AUDIO_PATH = "audio/";
    private static final String IMAGE_PATH = "image/";

    @SneakyThrows
    public void saveAudio(String filename, InputStream audioData) {
        if (StringUtils.isBlank(filename)) {
            return;
        }
        try (audioData) {
            File targetFile = new File(RESOURCES_PATH + AUDIO_PATH + filename);
            copyInputStreamToFile(audioData, targetFile);
        }
    }

    public void saveImageFromUrl(String filename, String urlPath) {
        if (StringUtils.isBlank(filename) || StringUtils.isBlank(urlPath)) {
            return;
        }
        try (InputStream inputStream = new URL(urlPath).openStream()) {
            Files.copy(inputStream, Paths.get(RESOURCES_PATH + IMAGE_PATH + filename));
            log.debug("Image file saved, filename: {}", filename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public void saveImageFromInputStream(String filename, InputStream photoData) {
        if (StringUtils.isBlank(filename)) {
            return;
        }
        try (photoData) {
            File targetFile = new File(RESOURCES_PATH + IMAGE_PATH + filename);
            copyInputStreamToFile(photoData, targetFile);
        }
    }

    public void deleteImageFile(String imageFilename) {
        if (StringUtils.isBlank(imageFilename)) {
            return;
        }
        try {
            Files.delete(Paths.get(RESOURCES_PATH + IMAGE_PATH + imageFilename));
            log.debug("Image file deleted: {}", imageFilename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAudioFile(String audioFilename) {
        if (StringUtils.isBlank(audioFilename)) {
            return;
        }
        try {
            Files.delete(Paths.get(RESOURCES_PATH + AUDIO_PATH + audioFilename));
            log.debug("Audio file deleted: {}", audioFilename);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
