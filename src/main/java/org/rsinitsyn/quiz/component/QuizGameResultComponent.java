package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.rsinitsyn.quiz.model.QuizGameStateModel;

public class QuizGameResultComponent extends VerticalLayout {

    private QuizGameStateModel quizGameStateModel;

    private H2 title = new H2();

    private H4 resultPercent = new H4();
    private H4 resultCount = new H4();
    private H4 reaction = new H4();

    public QuizGameResultComponent(QuizGameStateModel quizGameStateModel) {
        this.quizGameStateModel = quizGameStateModel;
        configureComponents();
        add(title, resultCount, resultPercent, reaction);
    }

    private void configureComponents() {
        title.setText("Результаты игрока: " + quizGameStateModel.getPlayerName());
        resultPercent.setText("Процент верных ответов: " + quizGameStateModel.calculateAndGetAnswersResult() + "%");
        resultCount.setText("Верных ответов: " + quizGameStateModel.getAnswersStatistic());
        reaction.setText(getResultReaction());
    }

    private String getResultReaction() {
        int res = quizGameStateModel.calculateAndGetAnswersResult();
        String reaction = "";
        if (res >= 90) {
            reaction = "Великолепно!";
        } else if (res >= 75) {
            reaction = "Достойно)";
        } else if (res >= 50) {
            reaction = "Так себе результат :/";
        } else if (res >= 25) {
            reaction = "Плохо...";
        } else {
            reaction = "Ты полное днище :))))00))";
        }
        return reaction;
    }
}
