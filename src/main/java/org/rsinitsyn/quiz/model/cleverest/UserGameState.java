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

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"bets"})
public class UserGameState implements Comparable<UserGameState> {
    private String username;
    private String color;

    private boolean lastWasCorrect;
    @Setter(AccessLevel.NONE)
    private String lastAnswerText;
    @Setter(AccessLevel.NONE)
    private LocalDateTime lastAnswerTime;
    private int lastPosition;
    private long lastResponseTime;
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
        lastAnswerTime = LocalDateTime.now();
        lastResponseTime = ChronoUnit.MILLIS.between(
                questionRenderTime,
                lastAnswerTime);
    }

    public void prepareForNext() {
        lastWasCorrect = false;
        lastAnswerText = "";
        answerGiven = false;
        lastAnswerTime = null;
        lastResponseTime = 0;
    }

    public void increaseBetScore() {
        betScore++;
    }

    public void increaseScore() {
        score++;
        lastWasCorrect = true;
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
                lastAnswerTime,
                lastPosition,
                lastResponseTime,
                score,
                answerGiven,
                bets,
                betScore,
                avgResponseTime);
    }
}
