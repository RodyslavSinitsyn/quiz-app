package org.rsinitsyn.quiz.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ViktorinaContext {
    private String id;
    private List<ViktorinaQuestion> questions;
}
