package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.rsinitsyn.quiz.model.QuizGameStateModel;

public class QuizGameResultComponent extends VerticalLayout {

    private QuizGameStateModel quizGameStateModel;

    private H2 title = new H2("Результаты");

    public QuizGameResultComponent(QuizGameStateModel quizGameStateModel) {
        this.quizGameStateModel = quizGameStateModel;
        add(title);
    }
}
