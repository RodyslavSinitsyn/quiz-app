package org.rsinitsyn.quiz.service;

import com.vaadin.flow.component.ComponentEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;
import org.rsinitsyn.quiz.model.UserStateSnapshot;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class CleverestBroadcasterTest {

    private final ComponentEventBus eventBus = mock(ComponentEventBus.class);
    private final CleverestBroadcaster broadcaster = new CleverestBroadcaster();

    private final String createdBy = "host";
    public final String color = "color";
    private String gameId;

    @BeforeEach
    void setUp() {
        gameId = UUID.randomUUID().toString();
        broadcaster.registerEventBus(gameId, eventBus);
    }

    @AfterEach
    void tearDown() {
        then(eventBus).shouldHaveNoMoreInteractions();
        broadcaster.cleanState(gameId);
    }

    @Test
    void creates_state() {
        // when
        broadcaster.createState(gameId, createdBy, List.of(), List.of(), List.of());

        // then
        baseAssertions();
    }

    @Test
    void sends_join_user_event() {
        // given
        createEmptyState();

        // when
        broadcaster.sendJoinUserEvent(gameId, "Alice", color, null, "Bob", "Charlie");

        // then
        baseAssertions();
        assertSoftly(softly -> {
            final var alice = broadcaster.getState(gameId).getUserState("Alice");
            softly.assertThat(alice).isNotNull();
            softly.assertThat(alice.getColor()).isEqualTo(color);
            softly.assertThat(alice.getBets()).hasSize(2);
        });

        then(eventBus).should().fireEvent(new UserJoinedEvent(gameId, "Alice"));
    }

    @Test
    void sends_players_ready_event() {
        // given
        createEmptyState();
        addUser("Alice");
        addUser("Bob");

        // when
        broadcaster.sendPlayersReadyEvent(gameId);

        // then
        baseAssertions();
        assertThat(broadcaster.getState(gameId).getUserState("Alice")).isNotNull();
        assertThat(broadcaster.getState(gameId).getUserState("Bob")).isNotNull();

        then(eventBus).should().fireEvent(new AllUsersReadyEvent(gameId, Set.of("Alice", "Bob")));
    }

    @Test
    void sends_all_answered_events_given_one_user_answered_correctly() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice");

        // when
        broadcaster.sendSubmitAnswerEventAndCheckScore(gameId, "Alice", q, "4", () -> true);

        // then
        baseAssertions();
        final var state = broadcaster.getState(gameId);
        final var alice = state.getUserState("Alice");

        assertSoftly(softly -> {
            softly.assertThat(alice.getScore()).isEqualTo(1);
            softly.assertThat(alice.getCorrectAnswersCount()).isEqualTo(1);
            softly.assertThat(alice.isLastWasCorrect()).isTrue();

            softly.assertThat(alice.getLastAnswerText()).isEqualTo("4");
            softly.assertThat(alice.isAnswerGiven()).isTrue();
            softly.assertThat(alice.getLastResponseTimeMs()).isGreaterThanOrEqualTo(0);
        });
        // and
        then(eventBus).should().fireEvent(new UserAnsweredEvent(gameId, "Alice", "0.0 сек.", 1));
        then(eventBus).should().fireEvent(new AllUsersAnsweredEvent(gameId, q, true, false, 1, 0));
    }

    @Test
    void sends_user_answered_events_given_one_of_two_users_answered_wrong() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice");
        addUser("Bob");

        // when
        broadcaster.sendSubmitAnswerEventAndCheckScore(gameId, "Alice", q, "22", () -> false);

        // then
        baseAssertions();
        final var state = broadcaster.getState(gameId);
        final var alice = state.getUserState("Alice");

        assertSoftly(softly -> {
            softly.assertThat(alice.getScore()).isEqualTo(0);
            softly.assertThat(alice.getCorrectAnswersCount()).isEqualTo(0);
            softly.assertThat(alice.isLastWasCorrect()).isFalse();

            softly.assertThat(alice.getLastAnswerText()).isEqualTo("22");
            softly.assertThat(alice.isAnswerGiven()).isTrue();
            softly.assertThat(alice.getLastResponseTimeMs()).isGreaterThanOrEqualTo(0);
        });
        // and
        then(eventBus).should().fireEvent(new UserAnsweredEvent(gameId, "Alice", "0.0 сек.", 1));
        then(eventBus).should(never()).fireEvent(any(AllUsersAnsweredEvent.class));
    }

    @Test
    void sends_round_info_event() {
        // given
        createEmptyState();

        // when
        broadcaster.sendNewRoundEvent(gameId);

        // then
        baseAssertions();
        then(eventBus).should().fireEvent(new RoundInfoEvent(gameId,
                1,
                "В первом раунде будут вопросы на разные темы и 4 варианта ответов."));
    }

    @Test
    void sends_get_question_event() {
        // given
        final var q1 = aQuestionModel()
                .text("q1 text1")
                .build();
        createStateWithQuestions(List.of(q1));

        // when
        broadcaster.sendGetQuestionEvent(gameId);

        // then
        baseAssertions();
        then(eventBus).should().fireEvent(new GetQuestionEvent(gameId, q1, 1, 1, 1));
    }

    @Test
    void sends_update_personal_score_event() {
        // given
        createEmptyState();

        // when
        broadcaster.sendUpdatePersonalScoreEvent(gameId);

        // then
        baseAssertions();
        then(eventBus).should().fireEvent(new UpdatePersonalScoreEvent(gameId));
    }

    @Test
    void sends_save_user_answers_event() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice", "4");

        // pre-condition
        assertSoftly(softly -> {
            final var alice = broadcaster.getState(gameId).getUserState("Alice");
            softly.assertThat(alice.getScore()).isEqualTo(0);
            softly.assertThat(alice.isAnswerGiven()).isTrue();
            softly.assertThat(alice.isLastWasCorrect()).isFalse();
            softly.assertThat(alice.getLastAnswerText()).isEqualTo("4");
            softly.assertThat(alice.getLastResponseTimeMs()).isNotNegative();

            softly.assertThat(broadcaster.getState(gameId).getHistory()).isEmpty();
        });

        // when
        broadcaster.sendSaveUsersAnswersEvent(gameId, q);

        // then
        baseAssertions();

        final var alice = broadcaster.getState(gameId).getUserState("Alice");
        assertSoftly(softly -> {
            softly.assertThat(alice.getScore()).isEqualTo(0);
            softly.assertThat(alice.isAnswerGiven()).isFalse();
            softly.assertThat(alice.isLastWasCorrect()).isFalse();
            softly.assertThat(alice.getLastAnswerText()).isEmpty();
            softly.assertThat(alice.getLastResponseTimeMs()).isEqualTo(0);
        });

        then(eventBus).should().fireEvent(new SaveUsersAnswersEvent(gameId,
                q,
                List.of(new UserStateSnapshot("Alice", color, "4", false, true, 0, 0))));
    }

    @Test
    void sends_game_finished_event_and_updates_state() {
        // given
        createStateWithQuestions(List.of(aQuestionModel().build()));
        addUser("Alice");

        // when
        broadcaster.sendFinishGameEvent(gameId);

        // then
        baseAssertions();

        final var state = broadcaster.getState(gameId);
        assertThat(state.getUserState("Alice").getLastPosition()).isGreaterThan(0);
//        assertThat(state.getUsers().get("Alice").getAvgResponseTime()).isNotNull(); // todo: check why null

        then(eventBus).should().fireEvent(new GameFinishedEvent(gameId));
    }

    @Test
    void sends_question_chosen_event() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice");

        // and
        final var initialQuestionRenderTime = broadcaster.getState(gameId).getQuestionRenderedTime();

        // when
        broadcaster.sendQuestionChoosenEvent(gameId, q, broadcaster.getState(gameId).getUserState("Alice"));

        // then
        baseAssertions();
        assertThat(broadcaster.getState(gameId).getQuestionRenderedTime())
                .isAfterOrEqualTo(initialQuestionRenderTime);

        // and
        then(eventBus).should().fireEvent(new QuestionChoosenEvent(gameId, q, "Alice"));
    }


    @Test
    void sends_question_graded_event() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice");

        // when
        broadcaster.sendQuestionGradedEvent(gameId, q, "Alice", 5);

        // then
        baseAssertions();
        then(eventBus).should().fireEvent(new QuestionGradedEvent(gameId, q, "Alice", 5));
    }

    private void baseAssertions() {
        assertThat(broadcaster.getState(gameId))
                .as("State should be created")
                .isNotNull()
                .extracting("createdBy").isEqualTo(createdBy);
    }

    private void createEmptyState() {
        broadcaster.createState(gameId, createdBy, List.of(), List.of(), List.of());
        broadcaster.getState(gameId).refreshQuestionRenderedTime();
    }

    private void createStateWithQuestions(List<QuestionModel> firstRoundQuestions) {
        broadcaster.createState(gameId, createdBy, new ArrayList<>(firstRoundQuestions), List.of(), List.of());
        broadcaster.getState(gameId).refreshQuestionRenderedTime();
    }

    private UserGameState addUser(String username) {
        return broadcaster.getState(gameId).addOrUpdateUser(gameId, username, color, null, "", "");
    }

    private UserGameState addUser(String username, String answer) {
        final var userGameState = addUser(username);
        userGameState.submitLatestAnswer(answer, LocalDateTime.now());
        return userGameState;
    }

    private QuestionModel.QuestionModelBuilder aQuestionModel() {
        return QuestionModel.builder()
                .id(UUID.randomUUID())
                .text("2 + 2 = ?")
                .type(QuestionType.TEXT)
                .categoryName("General")
                .answers(List.of(
                        AnswerModel.builder()
                                .number(0)
                                .text("4")
                                .correct(true)
                                .build(),
                        AnswerModel.builder()
                                .number(1)
                                .text("22")
                                .correct(false)
                                .build()
                ));
    }
}