package org.rsinitsyn.quiz.model.cleverest;

import lombok.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.Optional.empty;
import static org.rsinitsyn.quiz.entity.AnswerStatus.*;
import static org.rsinitsyn.quiz.utils.QuizUtils.divide;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = {"profile", "lastAnswerResult", "lastAnswerText", "score", "correctAnswersCount", "answerGiven"})
@ToString(exclude = "bets")
public class UserGameState implements Comparable<UserGameState> {
    @Getter(AccessLevel.NONE)
    private UserProfile profile;

    private AnswerStatus lastAnswerStatus;
    private String lastAnswerText;
    @Setter
    private int lastPosition;
    private long lastResponseTimeMs;
    private int correctAnswersCount;
    private int score = 0;
    private boolean answerGiven;
    private Map<String, MutablePair<String, Boolean>> bets = new HashMap<>();
    private int betScore;
    @Setter
    private Double avgResponseTime;

    public static UserGameState userGameState(String username,
                                              String color,
                                              byte[] photo) {
        final var userGameState = new UserGameState();
        userGameState.profile = new UserProfile(username, color, photo);
        userGameState.lastAnswerStatus = UNKNOWN;
        return userGameState;
    }

    public String getUsername() {
        return profile.username();
    }

    public String getColor() {
        return profile.color();
    }

    public byte[] getPhoto() {
        return profile.avatar();
    }

    public void updateColorAndPhoto(String color, byte[] photo) {
        this.profile = this.profile.withColorAndAvatar(color, photo);
    }

    public void submitAnswer(String answerText,
                             LocalDateTime questionRenderTime,
                             Supplier<AnswerResult> answerResult) {
        if (answerGiven) {
            return;
        }
        lastAnswerText = answerText;
        answerGiven = true;
        lastResponseTimeMs = MILLIS.between(questionRenderTime, now());

        final var result = answerResult.get();
        if (result.status() == UNKNOWN) {
            return;
        }
        if (result.status().correct()) {
            increaseScoreAndMarkCorrect(result.correctCount());
        } else {
            this.lastAnswerStatus = WRONG;
        }
    }

    public String getLastResponseTimeSec() {
        return "%.1f сек.".formatted(divide(lastResponseTimeMs, 1_000));
    }

    public void prepareForNext() {
        lastAnswerStatus = UNKNOWN;
        lastAnswerText = "";
        answerGiven = false;
        lastResponseTimeMs = 0;
    }

    public void increaseBetScore() {
        betScore++;
    }

    public void increaseScoreAndMarkCorrect(int score) {
        this.score += score;
        this.correctAnswersCount++;
        this.lastAnswerStatus = CORRECT; // todo: better logic to handle partial
    }

    public void decreaseScore(int score) {
        this.score -= score;
        this.lastAnswerStatus = WRONG;
    }

    public int totalScore() {
        return score + betScore;
    }

    public void updateBet(String bet, boolean isWinnerBet, boolean isBetRight) {
        if (isWinnerBet) {
            bets.put("winner", MutablePair.of(bet, isBetRight));
        } else {
            bets.put("loser", MutablePair.of(bet, isBetRight));
        }
    }

    public MutablePair<String, Boolean> winnerBet() {
        return bets.get("winner");
    }

    public MutablePair<String, Boolean> loserBet() {
        return bets.get("loser");
    }

    @Override
    public int compareTo(UserGameState other) {
        return Comparator.comparingInt(UserGameState::totalScore)
                .reversed()
                .compare(this, other);
    }

    public UserProfile profile() {
        return profile;
    }

    public UserStateSnapshot snapshot(Optional<UUID> questionId) {
        return new UserStateSnapshot(profile, lastAnswerText, lastAnswerStatus,
                answerGiven, lastResponseTimeMs, score, lastPosition, questionId);
    }

    public UserStateSnapshot snapshot() {
        return snapshot(empty());
    }
}
