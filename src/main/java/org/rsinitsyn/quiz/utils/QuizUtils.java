package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Slf4j
public final class QuizUtils {

    public static final String DATE_FORMAT_VALUE = "dd-MM-yyyy HH:mm:ss";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_VALUE);

    public static final String RESOURCES_PATH = "src/main/resources/";
    public static final String IMAGE_FOLDER = "image/";
    public static final String AUDIO_FOLDER = "audio/";

    private QuizUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // Date
    public static String formatDate(LocalDateTime dateTime) {
        return DATE_FORMAT.format(
                Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    public static double divide(double val, double divideOn, int afterDigit) {
        return BigDecimal.valueOf(val)
                .divide(BigDecimal.valueOf(NumberUtils.max(divideOn, 1)), afterDigit, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static double divide(double val, double divideOn) {
        return divide(val, divideOn, 2);
    }

    public static StreamResource createStreamResourceForAudio(String filename) {
        return new StreamResource(filename.split("/")[1], () -> {
            try {
                return new FileInputStream(readAudioFile(filename));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static StreamResource createStreamResourceForPhoto(String filename) {
        if (filename.split("/").length != 2) {
            return null;
        }
        return new StreamResource(filename.split("/")[1], () -> {
            try {
                return new FileInputStream(readImageFile(filename));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static StreamResource createStreamResourceForPhoto(String filename, InputStream inputStream) {
        return new StreamResource(filename, () -> inputStream);
    }

    public static String generateFilename(String urlPath) {
        String extension = StringUtils.defaultIfBlank(FilenameUtils.getExtension(urlPath), "jpg");
        extension = '.' + extension;
        return UUID.randomUUID() + extension;
    }

    public static String generateFilenameWithExt(String extension) {
        return "%s.%s".formatted(UUID.randomUUID(), extension);
    }

    @SneakyThrows
    public static File readFileFromResources(String pathToFile) {
        return org.springframework.util.ResourceUtils.getFile(RESOURCES_PATH + pathToFile);
    }


    @SneakyThrows
    public static File readImageFile(String pathToFile) {
        return readFileFromResources(IMAGE_FOLDER + pathToFile);
    }

    @SneakyThrows
    public static File readAudioFile(String pathToFile) {
        return readFileFromResources(AUDIO_FOLDER + pathToFile);
    }


    public static void runActionInUi(UI ui, Command action) {
        ui.access(action);
    }

    public static void runActionInUi(Optional<UI> maybeUi, Command action) {
        runActionInUi(maybeUi.orElseThrow(() -> new RuntimeException("UI not exists!")), action);
    }

    public static void logState(Component component,
                                UI ui,
                                String action,
                                boolean start,
                                Collection<?> subs) {
        logState(component, Optional.ofNullable(ui), action, start, subs);
    }

    public static void logState(Component component,
                                Optional<UI> ui,
                                String action,
                                boolean start,
                                Collection<?> subs) {
        log.info("[FIX][{}={}] {} [{}], User [{}], UI [{}], Subs size=[{}], items[{}]",
                component.getClass().getSimpleName(), component.hashCode(),
                start ? "Start" : "End", action, getLoggedUser(),
                ui.map(Object::hashCode).orElse(-1), subs.size(),
                subs);
    }

//
//    // todo temp
//    private void exportCode() {
//        String res = questionService.findAll().stream().
//                map(entity -> {
//                    List<AnswerEntity> answers = entity.getAnswers().stream()
//                            .sorted(Comparator.comparing(AnswerEntity::isCorrect, Comparator.reverseOrder()))
//                            .toList();
//                    StringJoiner joiner = new StringJoiner("|")
//                            .add(entity.getText())
//                            .add(answers.get(0).isCorrect() ? "_" + answers.get(0).getText() : answers.get(0).getText())
//                            .add(answers.get(1).isCorrect() ? "_" + answers.get(1).getText() : answers.get(1).getText())
//                            .add(answers.get(2).isCorrect() ? "_" + answers.get(2).getText() : answers.get(2).getText())
//                            .add(answers.get(3).isCorrect() ? "_" + answers.get(3).getText() : answers.get(3).getText());
//                    if (StringUtils.isNotEmpty(entity.getOriginalPhotoUrl())) {
//                        joiner.add(entity.getOriginalPhotoUrl());
//                    }
//                    return joiner.toString();
//                })
//                .collect(Collectors.joining("\n"));
//    }
}
