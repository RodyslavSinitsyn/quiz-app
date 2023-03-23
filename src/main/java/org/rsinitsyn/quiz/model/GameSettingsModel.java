package org.rsinitsyn.quiz.model;

import java.util.Set;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class GameSettingsModel {
    @Length(min = 1, max = 30)
    private String gameName;
    @Length(min = 1, max = 30)
    private String playerName;
    private Set<ViktorinaQuestion> questions;
}
