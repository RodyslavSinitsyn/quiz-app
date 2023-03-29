package org.rsinitsyn.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatsModel {
    private String username;
    private int questionsCreated;
    private int gamesCreated;
    private int gamesPlayed;
    private String answersStats;
    private String correctAnswersRate;
}
