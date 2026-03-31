package org.rsinitsyn.quiz.page;


import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;
import org.rsinitsyn.quiz.model.QuestionModel.HintModel;
import org.rsinitsyn.quiz.model.answer.AnswerResult;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.ThemeUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_CONTRAST;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;
import static org.rsinitsyn.quiz.entity.AnswerStatus.*;
import static org.rsinitsyn.quiz.entity.QuestionHintType.PHOTO;
import static org.rsinitsyn.quiz.entity.QuestionType.TEXT;

@Route(value = "/labs", layout = MainLayout.class)
@PageTitle("Labs")
@Slf4j
@PermitAll
public class LabsPage extends VerticalLayout {

    private final QuestionService questionService;

    public LabsPage(final QuestionService questionService) throws IOException {
        this.questionService = questionService;

        final var userGameState = UserGameState.userGameState("Rodyslav",
                "F54927",
                new FileInputStream("src/main/resources/image/dev/4704b5fb-a349-4f96-8fc0-240a30d10cca.jpg").readAllBytes());
        userGameState.submitAnswer("Lionel Messi", now(), () -> AnswerResult.oneOptionResult(true));

        final var users = List.of(userStateSnapshot("Alice", 1, CORRECT),
                userStateSnapshot("Bob", 2, PARTIAL),
                userStateSnapshot("Charlie", 3, WRONG));

        final var userAnswersLayout = userAnswersLayout(aQuestionModel(TEXT)
                .answerDescription("""
                        Mount Everest is the highest mountain in the world above sea level, reaching an elevation of 8,848.86 meters (29,032 feet) in the Himalayas. Located on the Nepal-China border, it is often called the "roof of the world". However, Mauna Kea in Hawaii is taller when measured from base to peak, and Chimborazo is further from Earth's center.\s
                        """)
                .build(), users, false, s -> {
        });
        openDialog(userAnswersLayout, "Ответы", () -> {});


        final var scoreTableLayout = usersScoreTableLayout(
                users,
                Map.of(
                        "Alice", List.of(CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT),
                        "Bob", List.of(CORRECT, CORRECT, WRONG, CORRECT, PARTIAL, CORRECT, UNKNOWN, CORRECT, CORRECT, CORRECT),
                        "Charlie", List.of(CORRECT, WRONG, WRONG, CORRECT, PARTIAL, CORRECT, PARTIAL, CORRECT, PARTIAL, UNKNOWN))
        );
        openDialog(scoreTableLayout, "Таблица результатов", () -> {
        });

        add(userProfile(userGameState.profile()));
        add(new Hr());


        add(userProfileWithScore(userGameState.snapshot()));
        add(new Hr());

        add(userProfileWithAnswer(userGameState.snapshot(), TEXT));
        add(new Hr());

        Arrays.stream(Emoji.values()).map(e -> e.value).toList().stream()
                .map(CleverestComponents::emoji)
                .forEach(this::add);

        final var questions = questionService.findAllByCurrentUserAsModel();
        if (questions.isEmpty()) {
            renderMockQuestions();
            return;
        }

        for (final var question : questions) {
            final var sequenceQuestion = createQuestionLayout(new QuestionLayoutRequest()
                    .host(false)
                    .question(question));
            add(sequenceQuestion);
            add(new Hr());

            sequenceQuestion.addAnsweredListener(e -> {
                final var event = e.getAnswerGivenEvent();
                final var text = "%s, %s, %d".formatted(
                        String.join(", ", event.getAnswers()),
                        event.getResult(),
                        0);
                notification(text, LUMO_CONTRAST);
            });
        }
    }

    private UserStateSnapshot userStateSnapshot(String username, int position, AnswerStatus status) {
        return new UserStateSnapshot(new UserProfile(username, ThemeUtils.BLACK_COLOR, null, Optional.empty()),
                randomText(1), status, true, 0, 0, position, Optional.empty());
    }

    private void renderMockQuestions() {
        for (final var questionType : QuestionType.values()) {

            if (questionType == QuestionType.PHOTO) {
                continue;
            }

            final var sequenceQuestion = createQuestionLayout(new QuestionLayoutRequest()
                    .host(true)
                    .question(aQuestionModel(questionType)
                            .hints(List.of(
                                    HintModel.builder().number(0).type(PHOTO).photoFilename("dev/a788c959-0872-41af-a7e9-b610e44e0cb3.jpg").build(),
                                    HintModel.builder().number(1).type(PHOTO).photoFilename("dev/f784a157-5647-437a-a679-2db9192c3c52.jpg").build(),
                                    HintModel.builder().number(2).type(PHOTO).photoFilename("dev/ced3dfc0-33e4-4e9e-b803-730faf01159b.jpg").build()
                            ))
                            .build())
            );
            try {
                add(sequenceQuestion);
                add(new Hr());
            } catch (Exception e) {
            }
        }
    }

    private static QuestionModel.QuestionModelBuilder aQuestionModel(final QuestionType questionType) {
        return QuestionModel.builder()
                .id(randomUUID())
                .categoryName(questionType + " = " + randomText(2))
                .text(randomText(5))
                .type(questionType)
                .answers(List.of(
                        new AnswerModel("Everest", true, 1, null),
                        new AnswerModel(randomText(7), false, 2, null),
                        new AnswerModel(randomText(20), false, 3, null),
                        new AnswerModel(randomText(35), false, 4, null)
                ));
    }

    public static String randomText(int wordCount) {
        final var generator = new RandomStringGenerator.Builder()
                .withinRange('a', 'z')
                .build();

        var builder = new StringBuilder();
        for (int i = 1; i <= wordCount; i++) {
            builder.append(generator.generate(5));
            if (i % 6 == 0) {      // после каждых 6 слов добавляем пробел
                builder.append(" ");
            } else if (i < wordCount) {
                builder.append(" ");  // обычный пробел между словами
            }
        }
        return builder.toString().trim();
    }
}
