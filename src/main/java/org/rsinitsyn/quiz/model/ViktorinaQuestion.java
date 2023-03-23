package org.rsinitsyn.quiz.model;

import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ViktorinaQuestion {

    private String text;
    private Set<ViktorinaAnswer> answers = new HashSet<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViktorinaAnswer {
        private String text;
        private boolean correct;
    }
}
