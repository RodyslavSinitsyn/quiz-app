package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.VaadinSession;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class QuizResourceUtils {

    private static final String RESOURCES_PATH = "src/main/resources/";

    public static final String AUDIO_PATH = "audio/";
    public static final String IMAGE_PATH = "image/";

    public String saveImageAndGetFilename(String urlPath) {
        String extension = StringUtils.defaultIfBlank(FilenameUtils.getExtension(urlPath), "jpg");
        extension = '.' + extension;
        try (InputStream inputStream = new URL(urlPath).openStream()) {
            String filename = UUID.randomUUID().toString();
            Files.copy(inputStream, Paths.get(RESOURCES_PATH + IMAGE_PATH + filename + extension));
            return filename + extension;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    public File getImageFile(String imageFilename) {
        return org.springframework.util.ResourceUtils.getFile(RESOURCES_PATH + IMAGE_PATH + imageFilename);
    }

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
