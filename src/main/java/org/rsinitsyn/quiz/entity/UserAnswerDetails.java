package org.rsinitsyn.quiz.entity;

import java.util.List;
import java.util.UUID;

import static org.apache.commons.lang3.Validate.validState;

public record UserAnswerDetails(
        List<String> answerTexts,
        List<UUID> answerIds,
        List<UUID> linkedAnswerIds
) {

    public static UserAnswerDetails from(List<String> answerTexts) {
        validState(!answerTexts.isEmpty());
        return new UserAnswerDetails(answerTexts, null, null);
    }

    public static UserAnswerDetails from(List<String> answerTexts,
                                         List<UUID> answerIds) {
        validState(!answerIds.isEmpty() || !answerTexts.isEmpty());
        return new UserAnswerDetails(answerTexts, answerIds, null);
    }

    public static UserAnswerDetails fromLink(List<String> answerTexts,
                                             List<UUID> answerIds,
                                             List<UUID> linkedAnswerIds) {
        validState(!answerIds.isEmpty() && !linkedAnswerIds.isEmpty());
        validState(answerIds.size() == linkedAnswerIds.size());
        return new UserAnswerDetails(answerTexts, answerIds, linkedAnswerIds);
    }

    public String getAnswersAsText() {
        return String.join(" ,", answerTexts);
    }
}
