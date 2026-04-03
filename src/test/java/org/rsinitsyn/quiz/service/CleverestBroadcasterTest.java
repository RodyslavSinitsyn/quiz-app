package org.rsinitsyn.quiz.service;

import com.vaadin.flow.component.ComponentEventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rsinitsyn.quiz.QuizTestFixture;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.answer.AnswerResult;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;
import org.rsinitsyn.quiz.service.CleverestBroadcaster.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.rsinitsyn.quiz.QuizTestFixture.aQuestionModel;
import static org.rsinitsyn.quiz.entity.AnswerStatus.*;

class CleverestBroadcasterTest implements QuizTestFixture {

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
    void sends_join_user_event_and_updates_state() {
        // given
        createEmptyState();

        // when
        broadcaster.sendJoinUserEvent(gameId, "Alice", color, null, "Bob", "Charlie");

        // then
        baseAssertions();
        assertSoftly(softly -> {
            final var alice = broadcaster.getState(gameId).getUserState("Alice");
            softly.assertThat(alice).isNotNull();
            softly.assertThat(alice.getBets()).hasSize(2);
        });

        then(eventBus).should().fireEvent(new UserJoinedEvent(gameId,
                new UserProfile("Alice", color, empty()),
                List.of(new UserProfile("Alice", color, empty()))));

        // and-when
        broadcaster.sendJoinUserEvent(gameId, "Alice", color, "photo-upd", "Bob", "Charlie");

        // then
        baseAssertions();
        assertSoftly(softly -> {
            final var alice = broadcaster.getState(gameId).getUserState("Alice");
            softly.assertThat(alice).isNotNull();
            softly.assertThat(alice.profile().photoUrl()).isEqualTo(of("photo-upd"));
            softly.assertThat(alice.getBets()).hasSize(2);
        });

        then(eventBus).should().fireEvent(new UserJoinedEvent(gameId,
                new UserProfile("Alice", color, of("photo-upd")),
                List.of(new UserProfile("Alice", color, of("photo-upd")))));
    }

    @Test
    void sends_players_ready_event() {
        // given
        createEmptyState();
        addUser("Alice");
        addUser("Bob");

        // when
        broadcaster.sendUsersReadyEvent(gameId);

        // then
        baseAssertions();
        assertThat(broadcaster.getState(gameId).getUserState("Alice")).isNotNull();
        assertThat(broadcaster.getState(gameId).getUserState("Bob")).isNotNull();

        then(eventBus).should().fireEvent(new AllUsersReadyEvent(gameId, Set.of("Alice", "Bob")));
    }

    @Test
    void sends_all_answered_events_given_one_user_answered_partially() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice");

        // when
        broadcaster.sendSubmitAnswerEventAndCheckScore(gameId, "Alice", q, "4", () -> new AnswerResult(PARTIAL, 3, 2));

        // then
        baseAssertions();
        final var state = broadcaster.getState(gameId);
        final var alice = state.getUserState("Alice");

        assertSoftly(softly -> {
            softly.assertThat(alice.getScore()).isEqualTo(2);
            softly.assertThat(alice.getCorrectAnswersCount()).isEqualTo(1);
            softly.assertThat(alice.getLastAnswerStatus()).isEqualTo(PARTIAL);
            softly.assertThat(alice.getLastAnswerText()).isEqualTo("4");
            softly.assertThat(alice.isAnswerGiven()).isTrue();
            softly.assertThat(alice.getLastResponseTimeMs()).isGreaterThanOrEqualTo(0);
        });
        // and
        then(eventBus).should().fireEvent(new UserAnsweredEvent(gameId, "Alice", "0.0 сек.", 1));
        then(eventBus).should().fireEvent(new AllUsersAnsweredEvent(gameId, q, true, false, 1, 0));
    }

    @Test
    void sends_user_answered_events_given_alice_wrong_bob_not_answered() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice");
        addUser("Bob");

        // when
        broadcaster.sendSubmitAnswerEventAndCheckScore(gameId, "Alice", q, "22", () -> new AnswerResult(WRONG, 1, 0));

        // then
        baseAssertions();
        final var state = broadcaster.getState(gameId);
        final var alice = state.getUserState("Alice");
        final var bob = state.getUserState("Bob");

        assertSoftly(softly -> {
            softly.assertThat(alice.getScore()).isEqualTo(0);
            softly.assertThat(alice.getCorrectAnswersCount()).isEqualTo(0);
            softly.assertThat(alice.getLastAnswerStatus()).isEqualTo(WRONG);
            softly.assertThat(alice.getLastAnswerText()).isEqualTo("22");
            softly.assertThat(alice.isAnswerGiven()).isTrue();
            softly.assertThat(alice.getLastResponseTimeMs()).isGreaterThanOrEqualTo(0);

            softly.assertThat(bob.getScore()).isEqualTo(0);
            softly.assertThat(bob.getCorrectAnswersCount()).isEqualTo(0);
            softly.assertThat(bob.getLastAnswerStatus()).isEqualTo(UNKNOWN);
            softly.assertThat(bob.getLastAnswerText()).isNull();
            softly.assertThat(bob.isAnswerGiven()).isFalse();
            softly.assertThat(bob.getLastResponseTimeMs()).isGreaterThanOrEqualTo(0);

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
                "Раунд 1"));
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
    void sends_save_user_answers_event_and_calculates_positions() {
        // given
        final var q = aQuestionModel().build();
        createStateWithQuestions(List.of(q));
        addUser("Alice", "4", new AnswerResult(CORRECT, 1, 1));
        addUser("Bob", "22", new AnswerResult(WRONG, 1, 1));

        // pre-condition
        assertSoftly(softly -> {
            final var alice = broadcaster.getState(gameId).getUserState("Alice");
            softly.assertThat(alice.getScore()).isEqualTo(1);
            softly.assertThat(alice.getCorrectAnswersCount()).isEqualTo(1);
            softly.assertThat(alice.isAnswerGiven()).isTrue();
            softly.assertThat(alice.getLastAnswerStatus()).isEqualTo(CORRECT);
            softly.assertThat(alice.getLastAnswerText()).isEqualTo("4");
            softly.assertThat(alice.getLastResponseTimeMs()).isNotNegative();

            final var bob = broadcaster.getState(gameId).getUserState("Bob");
            softly.assertThat(bob.getScore()).isEqualTo(0);
            softly.assertThat(bob.getCorrectAnswersCount()).isEqualTo(0);
            softly.assertThat(bob.isAnswerGiven()).isTrue();
            softly.assertThat(bob.getLastAnswerStatus()).isEqualTo(WRONG);
            softly.assertThat(bob.getLastAnswerText()).isEqualTo("22");
            softly.assertThat(bob.getLastResponseTimeMs()).isNotNegative();

            softly.assertThat(broadcaster.getState(gameId).getHistory()).isEmpty();
        });

        // when
        broadcaster.sendSaveUsersAnswersEvent(gameId, q);

        // then
        baseAssertions();

        final var alice = broadcaster.getState(gameId).getUserState("Alice");
        final var bob = broadcaster.getState(gameId).getUserState("Bob");

        // verify state cleaned after a flush to db
        assertSoftly(softly -> {
            softly.assertThat(alice.getScore()).isEqualTo(1);
            softly.assertThat(alice.getCorrectAnswersCount()).isEqualTo(1);
            softly.assertThat(alice.isAnswerGiven()).isFalse();
            softly.assertThat(alice.getLastAnswerStatus()).isEqualTo(UNKNOWN);
            softly.assertThat(alice.getLastAnswerText()).isEmpty();
            softly.assertThat(alice.getLastResponseTimeMs()).isEqualTo(0);

            softly.assertThat(bob.getScore()).isEqualTo(0);
            softly.assertThat(bob.getCorrectAnswersCount()).isEqualTo(0);
            softly.assertThat(bob.isAnswerGiven()).isFalse();
            softly.assertThat(bob.getLastAnswerStatus()).isEqualTo(UNKNOWN);
            softly.assertThat(bob.getLastAnswerText()).isEmpty();
            softly.assertThat(bob.getLastResponseTimeMs()).isEqualTo(0);
        });
        final var history = broadcaster.getState(gameId).getHistory();
        assertThat(history)
                .hasSize(1)
                .containsKey(q)
                .extractingByKey(q)
                .asInstanceOf(LIST)
                .hasSize(2);

        then(eventBus).should().fireEvent(new SaveUsersAnswersEvent(gameId,
                q,
                List.of(new UserStateSnapshot(
                                new UserProfile("Alice", color, empty()),
                                "4", CORRECT, true, 0, 1, 1, of(q.getId())),
                        new UserStateSnapshot(
                                new UserProfile("Bob", color, empty()),
                                "22", WRONG, true, 0, 0, 2, of(q.getId())))));
    }

    @Test
    void sends_game_finished_event_and_updates_state() {
        // given
        final var questionModel = aQuestionModel().build();
        createStateWithQuestions(List.of(questionModel));
        addUser("Alice", "4", new AnswerResult(CORRECT, 1, 1));

        // when
        broadcaster.sendFinishGameEvent(gameId);

        // then
        baseAssertions();

        final var state = broadcaster.getState(gameId);
        assertThat(state.getUserState("Alice").getLastPosition()).isGreaterThan(0);
//        assertThat(state.getUserState("Alice").getAvgResponseTime()).isNotNull();

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
        broadcaster.sendQuestionGradedEvent(gameId, q, "Alice", Emoji.GOOD);

        // then
        baseAssertions();
        then(eventBus).should().fireEvent(new QuestionGradedEvent(gameId, q, "Alice", 4, Emoji.GOOD));
    }

    private void baseAssertions() {
        assertThat(broadcaster.getState(gameId))
                .as("State should be created")
                .isNotNull()
                .extracting("gameHostName").isEqualTo(createdBy);
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

    private UserGameState addUser(String username, String answer, AnswerResult result) {
        final var userGameState = addUser(username);
        userGameState.submitAnswer(answer, LocalDateTime.now(), () -> result);
        return userGameState;
    }

}