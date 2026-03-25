package org.rsinitsyn.quiz.model.cleverest;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import lombok.*;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.model.UserStateSnapshot;

import static java.time.LocalDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static org.rsinitsyn.quiz.utils.QuizUtils.divide;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"username", "color", "lastWasCorrect", "lastAnswerText", "score", "correctAnswersCount", "answerGiven"})
@ToString(exclude = "bets")
public class UserGameState implements Comparable<UserGameState> {
    private String username;
    private String color;
    private InputStream photo;

    private boolean lastWasCorrect;
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

    public static UserGameState userGameState(String username, String color) {
        final var userGameState = new UserGameState();
        userGameState.username = username;
        userGameState.color = color;
        return userGameState;
    }

    public void updateColor(String color) {
        this.color = color;
    }

    public void submitLatestAnswer(String answerText, LocalDateTime questionRenderTime) {
        lastAnswerText = answerText;
        answerGiven = true;
        lastResponseTimeMs = MILLIS.between(questionRenderTime, now());
    }

    public String getLastResponseTimeSec() {
        return "%.1f сек.".formatted(divide(lastResponseTimeMs, 1_000));
    }

    public void prepareForNext() {
        lastWasCorrect = false;
        lastAnswerText = "";
        answerGiven = false;
        lastResponseTimeMs = 0;
    }

    public void increaseBetScore() {
        betScore++;
    }

    public void increaseScore() {
        score++;
        correctAnswersCount++;
        lastWasCorrect = true;
    }

    public void increaseScore(int score) {
        this.score += score;
        this.correctAnswersCount++;
        this.lastWasCorrect = true;
    }

    public void decreaseScore(int score) {
        this.score -= score;
        this.lastWasCorrect = false;
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

    public UserStateSnapshot snapshot() {
        return new UserStateSnapshot(username, color, lastAnswerText, lastWasCorrect, answerGiven, lastResponseTimeMs, score);
    }
}
