package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

    public static final String DATE_FORMAT_VALUE = "dd-MM-yyyy HH:mm:ss";
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat(DATE_FORMAT_VALUE);

    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String IMAGE_PATH = "image/";

    // Date
    public String formatDate(LocalDateTime dateTime) {
        return DATE_FORMAT.format(
                Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    public StreamResource createStreamResourceForPhoto(String photoFilename) {
        return new StreamResource(photoFilename, () -> {
            try {
                return new FileInputStream(readFile(photoFilename));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public String generateFilename(String urlPath) {
        String extension = StringUtils.defaultIfBlank(FilenameUtils.getExtension(urlPath), "jpg");
        extension = '.' + extension;
        return UUID.randomUUID() + extension;
    }

    @SneakyThrows
    public File readFile(String pathToFile) {
        return org.springframework.util.ResourceUtils.getFile(RESOURCES_PATH + IMAGE_PATH + pathToFile);
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
