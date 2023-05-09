package org.rsinitsyn.quiz.model.cleverest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.rsinitsyn.quiz.model.QuestionModel;

class CleverestGameStateTest {

    @ParameterizedTest
    @CsvSource(
            delimiter = ';',
            value = {
                    "3;0,0,0",
                    "4;0,0,0,0",
                    "5;0,0,0,0,0",
                    "6;1,0,1,0,1,0",
                    "7;1,0,1,0,1,0,0",
                    "8;1,0,1,0,1,0,1,0",
                    "9;2,1,0,2,1,0,2,1,0",
                    "10;2,1,0,2,1,0,2,1,0,0",
                    "15;4,3,2,1,0,4,3,2,1,0,4,3,2,1,0",
                    "30;9,8,7,6,5,4,3,2,1,0,9,8,7,6,5,4,3,2,1,0,9,8,7,6,5,4,3,2,1,0"
            }
    )
    void getQuestionsLeftToRevealScoreTable(String listSize, String returnedValues) {
        // given
        String[] valuesArr = returnedValues.split(",");
        if (valuesArr.length != Integer.parseInt(listSize)) {
            throw new IllegalStateException("Test setup not correct");
        }
        List<QuestionModel> mockList = new ArrayList<>();
        for (int i = 0; i < Integer.parseInt(listSize); i++) {
            mockList.add(mockQuestion());
        }
        var state = new CleverestGameState(
                "mock",
                mockList,
                null,
                null
        );

        // when-then
        Arrays.stream(valuesArr)
                .mapToInt(Integer::parseInt)
                .forEach(value -> assertQuestionsLeftToReveal(value, state));
    }

    private void assertQuestionsLeftToReveal(int expectedSize, CleverestGameState state) {
        Assertions.assertEquals(expectedSize, state.getQuestionsLeftToRevealScoreTable());
        state.prepareNextQuestionAndCheckIsLast();
    }

    private QuestionModel mockQuestion() {
        return QuestionModel.builder().build();
    }
}