package org.rsinitsyn.quiz.page.cleverest;

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
import org.rsinitsyn.quiz.component.cleverest.CleverestGameSettingsComponent;
import org.rsinitsyn.quiz.entity.GameType;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.rsinitsyn.quiz.utils.QuizUtils.logState;
import static org.rsinitsyn.quiz.utils.SessionWrapper.getLoggedUser;

@Route(value = "cleverest/setup", layout = MainLayout.class)
@PageTitle("Cleverest - Новая игра")
@PermitAll
@Slf4j
@PreserveOnRefresh
public class CleverestSetupPage extends VerticalLayout {

    private final QuestionService questionService;
    private final GameService gameService;
    private final CleverestBroadcaster broadcaster;

    private final CleverestGameSettingsComponent settingsComponent;
    private final List<Registration> subscriptions = new ArrayList<>();

    public CleverestSetupPage(QuestionService questionService,
                              GameService gameService,
                              CleverestBroadcaster broadcaster) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.broadcaster = broadcaster;

        settingsComponent = new CleverestGameSettingsComponent(questionService.findAllCreatedByCurrentUser());
        add(settingsComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", true, subscriptions);
        subscriptions.add(settingsComponent.addSettingsCompletedListener(event -> {
            final var newGameId = UUID.randomUUID().toString();
            gameService.createIfNotExists(newGameId, "Cleverest", GameType.CLEVEREST);
            broadcaster.createState(
                    newGameId,
                    getLoggedUser(),
                    event.getFirstRound().stream()
                            .map(questionService::toQuizQuestionModel)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll),
                    event.getSecondRound().stream()
                            .map(questionService::toQuizQuestionModel)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll),
                    event.getThirdRound().stream()
                            .map(questionService::toQuizQuestionModel)
                            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll)
            );
            attachEvent.getUI().navigate(CleverestWaitingPage.class, newGameId);
        }));
        logState(this, attachEvent.getUI(), "onAttach", false, subscriptions);
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        logState(this, detachEvent.getUI(), "onDetach", true, subscriptions);
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
        logState(this, detachEvent.getUI(), "onDetach", false, subscriptions);
    }
}
