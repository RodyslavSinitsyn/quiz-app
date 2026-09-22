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
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.service.GameQuestionService;
import org.rsinitsyn.quiz.service.GameService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static org.rsinitsyn.quiz.utils.ModelConverterUtils.toShuffledGameQuestionPairs;
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
    private final GameQuestionService gameQuestionService;
    private final CleverestBroadcaster broadcaster;

    private final CleverestGameSettingsComponent settingsComponent;
    private final List<Registration> subscriptions = new ArrayList<>();

    public CleverestSetupPage(QuestionService questionService,
                              GameService gameService,
                              GameQuestionService gameQuestionService,
                              CleverestBroadcaster broadcaster) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.gameQuestionService = gameQuestionService;
        this.broadcaster = broadcaster;

        settingsComponent = new CleverestGameSettingsComponent(questionService.findAllCreatedByCurrentUser());
        add(settingsComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logState(this, attachEvent.getUI(), "onAttach", true, subscriptions);
        subscriptions.add(settingsComponent.addSettingsCompletedListener(event -> {
            final var newGameId = UUID.randomUUID().toString();
            gameService.createIfNotExists(UUID.fromString(newGameId), event.getGameName(), GameType.CLEVEREST, empty());
            final var r1 = shuffleAndToModel(event.getFirstRound());
            final var r2 = shuffleAndToModel(event.getSecondRound());
            final var r3 = shuffleAndToModel(event.getThirdRound());
            // old approach of link questions with game
            gameService.linkQuestionsAndUsersWithGame(
                    newGameId,
                    Set.of(getLoggedUser()),
                    Stream.concat(Stream.concat(r1.stream(), r2.stream()), r3.stream()).toList());
            // new approach of link questions with game
            gameQuestionService.addQuestionsToGame(
                    UUID.fromString(newGameId),
                    Stream.concat(Stream.concat(
                            toShuffledGameQuestionPairs(r1, 1).stream(),
                            toShuffledGameQuestionPairs(r2, 2).stream()),
                            toShuffledGameQuestionPairs(r3, 3).stream()
                    ).toList());
            broadcaster.createState(newGameId, getLoggedUser(), r1, r2, r3);
            attachEvent.getUI().navigate(CleverestWaitingPage.class, newGameId);
        }));
        logState(this, attachEvent.getUI(), "onAttach", false, subscriptions);
    }

    private List<QuestionModel> shuffleAndToModel(List<QuestionEntity> questions) {
        final var questionModels = new ArrayList<>(questions.stream()
                .map(questionService::toQuizQuestionModel)
                .toList());
        Collections.shuffle(questionModels);
        return questionModels;
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        logState(this, detachEvent.getUI(), "onDetach", true, subscriptions);
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
        logState(this, detachEvent.getUI(), "onDetach", false, subscriptions);
    }
}
