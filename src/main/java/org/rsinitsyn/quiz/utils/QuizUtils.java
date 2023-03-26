package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class QuizUtils {

    public static final String RESOURCES_PATH = "src/main/resources/";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    public static final String AUDIO_PATH = "audio/";
    public static final String IMAGE_PATH = "image/";

    // Date
    public String formatDate(LocalDateTime dateTime) {
        return DATE_FORMAT.format(
                Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    // Image
    public String saveImageAndGetFilename(String urlPath) {
        String filename = generateFilename(urlPath);
        saveImage(filename, urlPath);
        return filename;
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

    public String generateFilename(String urlPath) {
        String extension = StringUtils.defaultIfBlank(FilenameUtils.getExtension(urlPath), "jpg");
        extension = '.' + extension;
        return UUID.randomUUID() + extension;
    }

    @SneakyThrows
    public File getImageFile(String imageFilename) {
        return org.springframework.util.ResourceUtils.getFile(RESOURCES_PATH + IMAGE_PATH + imageFilename);
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

    public StreamResource createStreamResourceForPhoto(String photoFilename) {
        return new StreamResource(photoFilename, () -> {
            try {
                return new FileInputStream(getImageFile(photoFilename));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Other Utils
    public String getLoggedUser() {
        return StringUtils.defaultIfEmpty(
                (String) VaadinSession.getCurrent().getAttribute("user"),
                "Аноним"
        );
    }

    @SneakyThrows
    public void sleep(int sec) {
        TimeUnit.SECONDS.sleep(sec);
    }
}
