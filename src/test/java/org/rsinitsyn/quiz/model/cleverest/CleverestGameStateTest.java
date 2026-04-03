package org.rsinitsyn.quiz.model.cleverest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.rsinitsyn.quiz.QuizTestFixture;
import org.rsinitsyn.quiz.model.QuestionModel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.rsinitsyn.quiz.QuizTestFixture.*;

class CleverestGameStateTest implements QuizTestFixture {

    @ParameterizedTest
    @CsvSource(
            delimiter = ';',
            value = {
                    "3;0,0,0",
//                    "4;0,0,0,0",
//                    "5;0,0,0,0,0",
                    "6;1,0,1,0,1,0",
//                    "7;1,0,1,0,1,0,0",
//                    "8;1,0,1,0,1,0,1,0",
                    "9;2,1,0,2,1,0,2,1,0",
//                    "10;2,1,0,2,1,0,2,1,0,0",
                    "15;4,3,2,1,0,4,3,2,1,0,4,3,2,1,0",
                    "30;9,8,7,6,5,4,3,2,1,0,9,8,7,6,5,4,3,2,1,0,9,8,7,6,5,4,3,2,1,0"
            }
    )
    void getCountToRevealScoreTable(String listSize, String returnedValues) {
        // given
        String[] valuesArr = returnedValues.split(",");
        if (valuesArr.length != Integer.parseInt(listSize)) {
            throw new IllegalStateException("Test setup not correct");
        }
        List<QuestionModel> questionList = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(listSize); i++) {
            questionList.add(aQuestionModel().build());
        }
        var state = new CleverestGameState(
                "mock",
                questionList,
                null,
                null
        );

        // when-then
        Arrays.stream(valuesArr)
                .mapToInt(Integer::parseInt)
                .forEach(value -> assertQuestionsLeftToReveal(value, state));
    }

    private void assertQuestionsLeftToReveal(int expectedSize, CleverestGameState state) {
        Assertions.assertEquals(expectedSize, state.getCountToRevealScoreTable());
        state.increaseQuestionNumber();
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = ';',
            value = {
                    "6;1,0,1,0,1,0;1,2,1,2,1,2",
                    "9;2,1,0,2,1,0,2,1,0;1,2,3,1,2,3,1,2,3",
                    "15;4,3,2,1,0,4,3,2,1,0,4,3,2,1,0;1,2,3,4,5,1,2,3,4,5,1,2,3,4,5"
            }
    )
    void shouldCalculateRevealStateAndLastAnswersWindow(
            String listSize,
            String revealValues,
            String windowValues
    ) {
        // given
        final var revealArr = revealValues.split(",");
        final var windowArr = windowValues.split(",");

        if (revealArr.length != Integer.parseInt(listSize)
                || windowArr.length != Integer.parseInt(listSize)) {
            throw new IllegalStateException("Test setup not correct");
        }

        final var questionList = new ArrayList<QuestionModel>();
        for (int i = 0; i < Integer.parseInt(listSize); i++) {
            questionList.add(aQuestionModel().build());
        }

        final var state = new CleverestGameState(
                "mock",
                questionList,
                null,
                null
        );

        // when-then
        for (int i = 0; i < revealArr.length; i++) {
            final var expectedReveal = Integer.parseInt(revealArr[i]);
            final var expectedWindow = Integer.parseInt(windowArr[i]);

            assertThat(state.getCountToRevealScoreTable()).isEqualTo(expectedReveal);
//            assertThat(state.updateQuestionsChunk()).isEqualTo(expectedWindow);

            state.increaseQuestionNumber();
        }
    }
}