package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.quiz.QuizGameSettingsComponent;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.SessionWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Route(value = "/quiz", layout = MainLayout.class)
@PageTitle("Game")
@PreserveOnRefresh
@PermitAll
public class QuizGameConfigurePage extends VerticalLayout {

    private QuizGameSettingsComponent settingsComponent;

    private QuestionService questionService;
    private GameService gameService;
    private UserService userService;

    private List<Registration> subscriptions = new ArrayList<>();

    public QuizGameConfigurePage(QuestionService questionService, GameService gameService, UserService userService) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.userService = userService;
        renderComponents();
    }

    private void renderComponents() {
        configureGameSettingsComponent();
    }

    private void configureGameSettingsComponent() {
        settingsComponent = new QuizGameSettingsComponent(
                questionService.findAllByCurrentUserAsModel(),
                userService.findAllExceptLogged());
        add(settingsComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscriptions.add(settingsComponent.addListener(QuizGameSettingsComponent.StartGameEvent.class, event -> {
            var newGameId = UUID.randomUUID().toString();
            gameService.createIfNotExists(newGameId, event.getGameState().getGameName(), GameType.QUIZ);
            gameService.linkQuestionsWithGame(newGameId, event.getGameState());
            getUI().ifPresent(ui -> ui.navigate(QuizGamePlayPage.class, newGameId));
        }));
        log.trace("onAttach. subscribe {}", subscriptions.size());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        log.trace("onDetach. unsubscribe {}", subscriptions.size());
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }
}
