package org.rsinitsyn.quiz.jobs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "quiz.job.uCarNumberGeneratorJob", havingValue = "true")
@Slf4j
public class UkranianCarNumbersGeneratorJob {

    private static final String SPLITERATOR = "|";

    @Scheduled(
            initialDelay = 0,
            fixedDelay = 3600,
            timeUnit = TimeUnit.SECONDS
    )
    public void runJob() {
        log.debug("Jon started.");

        String data = "AA\tКА\tг.Киев\n" +
                "AB\tКB\tВинницкая\n" +
                "AC\tКC\tВолынская\n" +
                "AE\tКE\tДнепропетровская\n" +
                "AH\tКH\tДонецкая\n" +
                "AI\tКI\tКиевская\n" +
                "AM\tКM\tЖитомирская\n" +
                "AO\tКO\tЗакарпатская\n" +
                "AP\tКP\tЗапорожская\n" +
                "AT\tКT\tИвано-Франковская\n" +
                "AX\tKX\tХарьковская\n" +
                "BA\tHA\tКировоградская\n" +
                "BB\tНВ\tЛуганская\n" +
                "BC\tНС\tЛьвовская\n" +
                "BE\tНЕ\tНиколаевская\n" +
                "BH\tНН\tОдесская\n" +
                "BI\tHI\tПолтавская\n" +
                "BK\tНК\tРовенская\n" +
                "BM\tНМ\tСумская\n" +
                "BO\tНО\tТернопольская\n" +
                "BT\tНТ\tХерсонская\n" +
                "BX\tНХ\tХмельницкая\n" +
                "CA\tIA\tЧеркасская\n" +
                "CB\tIB\tЧерниговская\n" +
                "CE\tIE\tЧерновицкая\n" +
                "АК\tКК\tАР Крым";
        processData(data);

        log.debug("Job finished.");
    }

    @SneakyThrows
    private void processData(String data) {
        StringJoiner res = new StringJoiner(System.lineSeparator());

        var map = new HashMap<String, String>();
        var options = new ArrayList<String>();

        data.lines().forEach(line -> {
            String[] tokens = line.split("\t");
            String number = tokens[0] + "/" + tokens[1];
            String oblast = tokens[2];
            map.put(number, oblast);
            options.add(oblast);
        });

        map.forEach((n, o) -> {
            var exclusions = new HashSet<String>();
            StringJoiner line = new StringJoiner(SPLITERATOR);
            line.add("Какой области эти номера " + n);

            line.add("_" + o);
            exclusions.add(o);

            // easy options also exclude
            exclusions.add("г.Киев");
            exclusions.add("Харьковская");
            exclusions.add("Киевская");

            String option2 = getRandomOption(options, exclusions);
            line.add(option2);
            exclusions.add(option2);

            String option3 = getRandomOption(options, exclusions);
            line.add(option3);
            exclusions.add(option3);

            String option4 = getRandomOption(options, exclusions);
            line.add(option4);

            res.add(line.toString());
        });

        log.debug("Result of job: {}", res);

        Files.writeString(Path.of("src/main/resources/jobs/import-ucn.txt"), res.toString());
    }

    @SneakyThrows
    private String getRandomOption(List<String> source, Set<String> exclusions) {
        while (true) {
            TimeUnit.MILLISECONDS.sleep(100); // for better random
            Collections.shuffle(source);
            var randomVal = source.subList(0, 1).get(0);
            if (!exclusions.contains(randomVal)) {
                return randomVal;
            }
            log.debug("Generate same: {}, ex: {}", randomVal, exclusions);
        }
    }
}
