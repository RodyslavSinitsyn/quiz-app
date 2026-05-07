package org.rsinitsyn.quiz.page;


import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.component.custom.AnimatedLeaderboardComponent;
import org.rsinitsyn.quiz.component.custom.LinkAnswersComponent;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerType;
import org.rsinitsyn.quiz.model.QuestionModel.HintModel;
import org.rsinitsyn.quiz.model.answer.AnswerResult;
import org.rsinitsyn.quiz.model.cleverest.ManualApprove;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.rsinitsyn.quiz.model.sound.GameSounds;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static com.vaadin.flow.component.notification.NotificationVariant.*;
import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;
import static org.rsinitsyn.quiz.entity.AnswerStatus.*;
import static org.rsinitsyn.quiz.entity.QuestionHintType.PHOTO;
import static org.rsinitsyn.quiz.entity.QuestionType.LINK;
import static org.rsinitsyn.quiz.entity.QuestionType.TEXT;
import static org.rsinitsyn.quiz.utils.ThemeUtils.BLACK_COLOR;

@Route(value = "/labs", layout = MainLayout.class)
@PageTitle("Labs")
@Slf4j
@PermitAll
public class LabsPage extends VerticalLayout {

    private final QuestionService questionService;

    public LabsPage(final QuestionService questionService) throws IOException {
        this.questionService = questionService;

        final var userGameState = UserGameState.userGameState("Rodyslav",
                SessionWrapper.getLoggedUserThemeColor(),
                "/dev/4704b5fb-a349-4f96-8fc0-240a30d10cca.jpg");
        userGameState.submitAnswer("Lionel Messi", now(), () -> AnswerResult.oneOptionResult(true));

        final var users = List.of(userStateSnapshot("Alice", 1, CORRECT),
                userStateSnapshot("Bob", 2, PARTIAL),
                userStateSnapshot("Charlie", 3, WRONG));

        openDialog(CleverestComponents.userAnswersLayout(aQuestionModel(TEXT).build(), users,
                Optional.of(new ManualApprove(
                        5,
                        (u) -> notification("%s +1".formatted(u), LUMO_SUCCESS, Notification.Position.TOP_STRETCH),
                        (u) -> notification("%s -1".formatted(u), LUMO_ERROR, Notification.Position.TOP_STRETCH)))
        ), "Results", () -> {
        });

        add(new LinkAnswersComponent(aQuestionModel(LINK)
                .answers(List.of(
                        AnswerModel.builder().type(AnswerType.TEXT).text("Один").number(1).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Два").number(2).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Три").number(3).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Четыре").number(4).correct(true).build(),

                        AnswerModel.builder().type(AnswerType.TEXT).text("One").number(1).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Two").number(2).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Three").number(3).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Four").number(4).correct(false).build()
                ))
                .build()));

        add(new Hr());

        add(new LinkAnswersComponent(aQuestionModel(LINK)
                .answers(List.of(
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/2a54eb19-cb6e-4d5c-9c56-77f994b37658.jpeg").number(1).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/3ff9006b-a710-4547-859e-8329f308045a.jpg").number(2).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/6c96ba5f-eef7-4c91-890d-697c14c19109.jpeg").number(3).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/6d9c045d-482e-4a9d-a463-c1dc220303a5.jpeg").number(4).correct(true).build(),

                        AnswerModel.builder().type(AnswerType.TEXT).text("Arsenal").number(1).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("PSG").number(2).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Barsa").number(3).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.TEXT).text("Pourtugal").number(4).correct(false).build()
                ))
                .build()));

        add(new Hr());

        add(new LinkAnswersComponent(aQuestionModel(LINK)
                .answers(List.of(
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/2a54eb19-cb6e-4d5c-9c56-77f994b37658.jpeg").number(1).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/3ff9006b-a710-4547-859e-8329f308045a.jpg").number(2).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/6c96ba5f-eef7-4c91-890d-697c14c19109.jpeg").number(3).correct(true).build(),
                        AnswerModel.builder().type(AnswerType.PHOTO).photoFilename("dev/6d9c045d-482e-4a9d-a463-c1dc220303a5.jpeg").number(4).correct(true).build(),

                        AnswerModel.builder().type(AnswerType.AUDIO).audioFilename("dev/8ee8ff90-239c-411e-b8f8-bccfaa5c923f.mp3").number(1).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.AUDIO).audioFilename("dev/9f7eed38-0e10-41a9-aeac-027379f4f7d5.mp3").number(2).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.AUDIO).audioFilename("dev/35e5afc6-1b6b-4026-b3ea-15bfdd4e588d.mp3").number(3).correct(false).build(),
                        AnswerModel.builder().type(AnswerType.AUDIO).audioFilename("dev/76ce581e-dc13-4af5-ab17-15f630457faa.mp3").number(4).correct(false).build()
                ))
                .build()));

        add(CleverestComponents.soundButton(() -> AudioUtils.playStaticSoundAsync(GameSounds.next().fullPath())));

        final var animatedLeaderboardComponent = new AnimatedLeaderboardComponent(
                List.of(
                        userStateSnapshot("Alice", 0, CORRECT),
                        userStateSnapshot("Bob", 0, CORRECT),
                        userStateSnapshot("Charlie", 0, CORRECT)
                ),
                List.of(
                        List.of(
                                userStateSnapshot("Alice", 1, CORRECT),
                                userStateSnapshot("Bob", 2, CORRECT),
                                userStateSnapshot("Charlie", 3, CORRECT)
                        ),
                        List.of(
                                userStateSnapshot("Alice", 1, CORRECT),
                                userStateSnapshot("Bob", 3, CORRECT),
                                userStateSnapshot("Charlie", 2, CORRECT)
                        ),
                        List.of(
                                userStateSnapshot("Alice", 1, CORRECT),
                                userStateSnapshot("Bob", 3, CORRECT),
                                userStateSnapshot("Charlie", 2, CORRECT)
                        ),
                        List.of(
                                userStateSnapshot("Alice", 2, CORRECT),
                                userStateSnapshot("Bob", 3, CORRECT),
                                userStateSnapshot("Charlie", 1, CORRECT)
                        ),
                        List.of(
                                userStateSnapshot("Alice", 3, CORRECT),
                                userStateSnapshot("Bob", 1, CORRECT),
                                userStateSnapshot("Charlie", 2, CORRECT)
                        ),
                        List.of(
                                userStateSnapshot("Alice", 1, CORRECT),
                                userStateSnapshot("Bob", 3, CORRECT),
                                userStateSnapshot("Charlie", 2, CORRECT)
                        )
                ),
                Duration.ofSeconds(2)
        );
        final var dialog = openDialog(animatedLeaderboardComponent, "Leaderboard", () -> {
        });
        dialog.setWidthFull();


        final var userAnswersLayout = userAnswersLayout(aQuestionModel(TEXT)
                .answerDescription("""
                        Mount Everest is the highest mountain in the world above sea level, reaching an elevation of 8,848.86 meters (29,032 feet) in the Himalayas. Located on the Nepal-China border, it is often called the "roof of the world". However, Mauna Kea in Hawaii is taller when measured from base to peak, and Chimborazo is further from Earth's center.\s
                        """)
                .build(), users, Optional.empty());
//        openDialog(userAnswersLayout, "Ответы", () -> {});


        final var scoreTableLayout = usersScoreTableLayout(
                users,
                Map.of(
                        "Alice", List.of(CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT, CORRECT),
                        "Bob", List.of(CORRECT, CORRECT, WRONG, CORRECT, PARTIAL, CORRECT, UNKNOWN, CORRECT, CORRECT, CORRECT),
                        "Charlie", List.of(CORRECT, WRONG, WRONG, CORRECT, PARTIAL, CORRECT, PARTIAL, CORRECT, PARTIAL, UNKNOWN))
        );
//        openDialog(scoreTableLayout, "Таблица результатов", () -> {
//        });

        add(userProfileWithScore(userGameState.snapshot()));
        add(new Hr());

        if (false) {
            renderMockQuestions();
            return;
        }

        final var questions = questionService.findAllByCurrentUserAsModel()
                .stream()
                .limit(50)
                .toList();
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
                notification(text, LUMO_CONTRAST, Notification.Position.TOP_STRETCH);
            });
        }
    }

    private UserStateSnapshot userStateSnapshot(String username, int position, AnswerStatus status) {
        return new UserStateSnapshot(new UserProfile(username, BLACK_COLOR, Optional.of("")),
                randomText(1), status, true, 0, randomInt(), position, Optional.empty());
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
                        new AnswerModel("Everest", true, 1, null, null, AnswerType.TEXT),
                        new AnswerModel(randomText(7), false, 2, null, null, AnswerType.TEXT),
                        new AnswerModel("Ответ который состоит из нескольких слов", false, 3, null, null, AnswerType.TEXT),
                        new AnswerModel("Вполне себе такой длинный вариант ответа, в нем даже есть запятая", false, 4, null, null, AnswerType.TEXT)
                ));
    }

    public static int randomInt() {
        return new Random().nextInt(20);
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
