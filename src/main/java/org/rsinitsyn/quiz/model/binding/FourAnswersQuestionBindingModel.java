package org.rsinitsyn.quiz.model.binding;

import jakarta.validation.constraints.NotBlank;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * Data model for Create/Edit/Delete quiz question.
 */
@Data
@NoArgsConstructor
public class FourAnswersQuestionBindingModel extends AbstractQuestionBindingModel {
    private String id;
    private String category;
    private String author;
    private InputStream audio;
    private List<AnswerBindingModel> answers = new ArrayList<>();

    public FourAnswersQuestionBindingModel(String id,
                                           String text,
                                           List<AnswerBindingModel> answers,
                                           String category,
                                           String author,
                                           String photoLocation,
                                           String answerDescriptionText) {
        super(id, text, answerDescriptionText, photoLocation);
        this.answers = answers;
        this.category = category;
        this.author = author;
    }

    public void initWith4Answers() {
        IntStream.range(0, 4)
                .mapToObj(index -> new AnswerBindingModel(null, index == 0, "", index))
                .forEach(answerBindingModel -> answers.add(answerBindingModel));
    }

    public boolean optionsRepeated() {
        return answers.size()
                != answers.stream().map(AnswerBindingModel::getText).collect(Collectors.toSet()).size();
    }

    public boolean noCorrectOption() {
        return answers.stream().noneMatch(FourAnswersQuestionBindingModel.AnswerBindingModel::isCorrect);
    }

    public boolean hasMultiCorrectOptions() {
        return answers.stream().filter(AnswerBindingModel::isCorrect)
                .count() > 1;
    }

    @Data
    @AllArgsConstructor
    public static class AnswerBindingModel {
        private UUID id;
        private boolean correct;
        @NotBlank
        @Length(min = 1, max = 255)
        private String text;
        private int index;
    }
}
