package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import org.rsinitsyn.quiz.component.GameSettingsComponent;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.springframework.beans.factory.annotation.Autowired;

@PreserveOnRefresh
@Route(value = "/game", layout = MainLayout.class)
@PageTitle("Game")
public class GamePage extends VerticalLayout implements HasUrlParameter<String>, AfterNavigationObserver {

    private String gameId;

    private GameSettingsComponent gameSettingsComponent;

    private QuestionDao questionDao;

    @Autowired
    public GamePage(QuestionDao questionDao) {
        this.questionDao = questionDao;
    }

    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        gameId = parameter;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        if (event.isRefreshEvent()) {
            return;
        }
        configureGameSettingsModel();
        add(gameSettingsComponent);
    }

    private void configureGameSettingsModel() {
        gameSettingsComponent = new GameSettingsComponent(ModelConverterUtils.toQuestionModels(questionDao.findAllWithAnswers()));
        gameSettingsComponent.addListener(GameSettingsComponent.StartGameEvent.class, event -> {
            remove(gameSettingsComponent);
            add(new Span("Game started..."),
                    new Span(event.getModel().getGameName()));
        });
    }
}
