package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.StreamResource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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

    public double divide(double val, double divideOn) {
        return BigDecimal.valueOf(val)
                .divide(BigDecimal.valueOf(NumberUtils.max(divideOn, 1)), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public StreamResource createStreamResourceForPhoto(String photoFilename) {
        return new StreamResource(photoFilename, () -> {
            try {
                return new FileInputStream(readImageFile(photoFilename));
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

    public String generateFilenameWithExt(String extension) {
        return UUID.randomUUID() + extension;
    }

    @SneakyThrows
    public File readImageFile(String pathToFile) {
        return readFileFromResources(IMAGE_PATH + pathToFile);
    }

    @SneakyThrows
    public File readFileFromResources(String pathToFile) {
        return org.springframework.util.ResourceUtils.getFile(RESOURCES_PATH + pathToFile);
    }

    public void runActionInUi(Optional<UI> optUi, Command action) {
        UI ui = optUi.orElseThrow(() -> new RuntimeException("UI not exists!"));
        ui.access(action);
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
