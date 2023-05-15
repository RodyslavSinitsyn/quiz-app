package org.rsinitsyn.quiz.model.cleverest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"bets"})
public class UserGameState implements Comparable<UserGameState> {
    private String username;
    private String color;

    private boolean lastWasCorrect;
//    @Setter(AccessLevel.NONE)
    private String lastAnswerText;
    private int lastPosition;
    private long lastResponseTime;
    @Setter(AccessLevel.NONE)
    private int correctAnswersCount;
    @Setter(AccessLevel.NONE)
    private int score = 0;
    @Setter(AccessLevel.NONE)
    private boolean answerGiven;
    private Map<String, MutablePair<String, Boolean>> bets = new HashMap<>();
    private int betScore;
    private Double avgResponseTime;

    public void submitLatestAnswer(String answerText, LocalDateTime questionRenderTime) {
        lastAnswerText = answerText;
        answerGiven = true;
        lastResponseTime = ChronoUnit.MILLIS.between(
                questionRenderTime,
                LocalDateTime.now());
    }

    public String lastResponseTimeSec() {
        return QuizUtils.divide(
                lastResponseTime,
                1_000) + " сек.";
    }

    public void prepareForNext() {
        lastWasCorrect = false;
        lastAnswerText = "";
        answerGiven = false;
        lastResponseTime = 0;
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

    public UserGameState copy() {
        return new UserGameState(
                username,
                color,
                lastWasCorrect,
                lastAnswerText,
                lastPosition,
                lastResponseTime,
                correctAnswersCount,
                score,
                answerGiven,
                bets,
                betScore,
                avgResponseTime);
    }
}
