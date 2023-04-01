package org.rsinitsyn.quiz.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResourceService {
    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String AUDIO_PATH = "audio/";
    private static final String IMAGE_PATH = "image/";

    private final QuizAppProperties properties;

    public void saveAudio(String filename) {
//        if (StringUtils.isBlank(filename) || StringUtils.isBlank(urlPath)) {
//            return;
//        }
//        try (InputStream inputStream = new URL(urlPath).openStream()) {
//            Files.copy(inputStream, Paths.get(RESOURCES_PATH + IMAGE_PATH + filename));
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
    }

    public void saveImage(String filename, String urlPath) {
        if (StringUtils.isBlank(filename) || StringUtils.isBlank(urlPath)) {
            return;
        }
        try (InputStream inputStream = new URL(urlPath).openStream()) {
            Files.copy(inputStream, Paths.get(RESOURCES_PATH + IMAGE_PATH + filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteImageFile(String imageFilename) {
        if (StringUtils.isBlank(imageFilename)) {
            return;
        }
        try {
            Files.delete(Paths.get(RESOURCES_PATH + IMAGE_PATH + imageFilename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
