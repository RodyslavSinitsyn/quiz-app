package org.rsinitsyn.quiz.model;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.entity.QuestionHintType;
import org.rsinitsyn.quiz.entity.QuestionType;

import java.util.*;
import java.util.stream.Collectors;

import static org.rsinitsyn.quiz.entity.QuestionHintType.PHOTO;
import static org.rsinitsyn.quiz.entity.QuestionHintType.TEXT;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@ToString(of = {"id", "text"})
public class QuestionModel {
    private UUID id;
    private String text;
    private QuestionType type;
    @Getter(AccessLevel.NONE)
    private String photoFilename;
    @Getter(AccessLevel.NONE)
    private String audioFilename;
    private String categoryName;
    private boolean optionsOnly;
    private Integer validRange;
    @Getter(AccessLevel.NONE)
    private String answerDescription;
    private Map<String, AnswerHistory> playersAnswersHistory;
    private List<AnswerModel> answers;
    private List<HintModel> hints;

    // for cleverest, mutable
    @Setter
    private boolean alreadyAnswered;
    @Setter
    private int points;

    public Optional<String> photoFilename() {
        return Optional.ofNullable(photoFilename).filter(StringUtils::isNoneBlank);
    }

    public Optional<String> audioFilename() {
        return Optional.ofNullable(audioFilename).filter(StringUtils::isNoneBlank);
    }

    public Optional<String> answerDescription() {
        return Optional.ofNullable(answerDescription).filter(StringUtils::isNoneBlank);
    }

    public AnswerModel getFirstCorrectAnswer() {
        return answers.stream()
                .sorted(Comparator.comparing(AnswerModel::number))
                .filter(AnswerModel::correct)
                .findFirst().orElseThrow();
    }

    public String getCorrectAnswersAsText() {
        if (!type.equals(QuestionType.LINK)) {
            return answers.stream()
                    .sorted(Comparator.comparing(AnswerModel::number))
                    .filter(AnswerModel::correct)
                    .map(AnswerModel::text)
                    .collect(Collectors.joining(System.lineSeparator()));
        } else {
            return answers.stream()
                    .collect(Collectors.groupingBy(AnswerModel::number,
                            LinkedHashMap::new,
                            Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    answerModels -> MutablePair.of(
                                            answerModels.stream().filter(AnswerModel::correct).findFirst().orElseThrow(),
                                            answerModels.stream().filter(am -> !am.correct()).findFirst().orElseThrow()
                                    )
                            )))
                    .values()
                    .stream()
                    .map(pair -> pair.getLeft().text() + " = " + pair.getRight().text())
                    .collect(Collectors.joining(System.lineSeparator()));
        }
    }

    public List<AnswerModel> getShuffledAnswers() {
        List<AnswerModel> answerList = new ArrayList<>(answers);
        Collections.shuffle(answerList);
        return answerList;
    }

    @Builder
    public record AnswerModel(String text,
                              boolean correct,
                              int number,
                              String photoFilename) {
    }

    @Builder
    public record HintModel(String text, String photoFilename, QuestionHintType type, int number) {

        public boolean textType() {
            return type == TEXT;
        }

        public boolean photoType() {
            return type == PHOTO;
        }
    }
}
