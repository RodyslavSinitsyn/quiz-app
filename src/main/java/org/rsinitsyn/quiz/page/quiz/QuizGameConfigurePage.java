package org.rsinitsyn.quiz.page.quiz;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.quiz.QuizGameSettingsComponent;
import org.rsinitsyn.quiz.entity.GameConfiguration;
import org.rsinitsyn.quiz.entity.GameQuestionMetadata;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.service.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.ofNullable;
import static org.rsinitsyn.quiz.entity.GameType.QUIZ;

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
    private GameQuestionService gameQuestionService;
    private GameParticipantService gameParticipantService;

    private List<Registration> subscriptions = new ArrayList<>();

    public QuizGameConfigurePage(QuestionService questionService,
                                 GameService gameService,
                                 UserService userService,
                                 GameQuestionService gameQuestionService,
                                 GameParticipantService gameParticipantService) {
        this.questionService = questionService;
        this.gameService = gameService;
        this.userService = userService;
        this.gameQuestionService = gameQuestionService;
        this.gameParticipantService = gameParticipantService;
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
        subscriptions.add(settingsComponent.addStartGameListener(event -> {
            var newGameId = UUID.randomUUID().toString();
            gameService.createIfNotExists(newGameId, event.getGameState().getGameName(), QUIZ, ofNullable(GameConfiguration.builder()
                    .hintsEnabled(event.getGameState().isHintsEnabled())
                    .optionsEnabled(event.getGameState().isAnswerOptionsEnabled())
                    .intrigueEnabled(event.getGameState().isIntrigueEnabled())
                    .build()));
            // old approach of assigning questions to game
            gameService.linkQuestionsWithGame(newGameId, event.getGameState());
            // new approach of assigning questions to game
            gameQuestionService.addQuestionsToGame(UUID.fromString(newGameId), convertToQuizQuestions(event.getGameState()));
            // add player to game
            gameParticipantService.addPlayer(UUID.fromString(newGameId), event.getGameState().getPlayerName());

            getUI().ifPresent(ui -> ui.navigate(QuizGamePlayPage.class, newGameId));
        }));
        log.trace("onAttach. subscribe {}", subscriptions.size());
    }

    // todo: move to mapper utility class
    private List<Pair<UUID, GameQuestionMetadata>> convertToQuizQuestions(final QuizGameState gameState) {
        final var counter = new AtomicInteger(0);
        return gameState.getQuestions().stream()
                .map(q -> {
                    final var metadata = GameQuestionMetadata.builder()
                            .order(counter.incrementAndGet())
                            .round(1)
                            .build();
                    return Pair.of(q.getId(), metadata);
                })
                .toList();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        log.trace("onDetach. unsubscribe {}", subscriptions.size());
        subscriptions.forEach(Registration::remove);
        subscriptions.clear();
    }
}
