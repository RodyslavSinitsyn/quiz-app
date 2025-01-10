package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.AnswerType;
import org.rsinitsyn.quiz.model.cleverest.CleverestGameState;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.service.CleverestBroadcaster;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;
import org.vaadin.addons.pandalyte.VoiceRecognition;

@Slf4j
public class CleverestGamePlayBoardComponent extends VerticalLayout {

    private String gameId;
    private CleverestBroadcaster broadcaster;
    private boolean isAdmin;
    private Runnable adminAction = null;

    private final Div topContainer = new Div();
    private final Div midContainer = new Div();
    private final Div botContainer = new Div();
    private final CleverestResultComponent resultComponent = new CleverestResultComponent();

    private final List<Registration> subs = new ArrayList<>();

    public void setState(String gameId, CleverestBroadcaster broadcaster, boolean isAdmin, boolean refreshEvent) {
        log.debug("Render components: {}", gameId);
        this.gameId = gameId;
        this.broadcaster = broadcaster;
        this.isAdmin = isAdmin;

        if (isAdmin) {
            int currRound = broadcaster.getState(gameId).getRoundNumber();
            if (refreshEvent) {
                showRoundRules(currRound, "Рефреш страницы.");
            } else {
                showRoundRules(currRound, broadcaster.getState(gameId).getRoundRules().get(currRound));
            }
        } else {
            renderUserPersonalScore();
        }

        topContainer.setWidthFull();
        midContainer.setWidthFull();
        botContainer.setWidthFull();
        add(topContainer, midContainer, botContainer);
    }

    private void renderTopContainerForAdmin(Collection<UserGameState> userGameStates) {
        if (!isAdmin) {
            return;
        }
        topContainer.removeAll();
        userGameStates.forEach(uState -> {
            Div userDiv = new Div();
            userDiv.getStyle().set("color", uState.getColor());
            userDiv.add(CleverestComponents.userNameSpan(uState.getUsername(), uState.getColor()));
            userDiv.setId("top-container-user-" + uState.getUsername());
            userDiv.setWidthFull();
            userDiv.addClassNames(LumoUtility.Border.BOTTOM, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.XXLARGE);
            topContainer.add(userDiv);
        });
    }

    private void updateUserAnswerGiven(UserGameState userGameState) {
        var component = (Div) topContainer.getChildren()
                .filter(c -> c.getId().orElseThrow().equals("top-container-user-" + userGameState.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cant update topContainer. Username not found: " + userGameState.getUsername()));
        component.addComponentAsFirst(CleverestComponents.userCheckIcon());
        component.addComponentAsFirst(QuizComponents.appendTextBorder(new Span(userGameState.lastResponseTimeSec())));
    }

    private void showRoundRules(int roundNumber, String rulesText) {
        Div rules = new Div();
        rules.setText(rulesText);
        rules.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.TextAlignment.CENTER);
        rules.setWidth("25em");
        CleverestComponents.openDialog(rules, "Раунд " + roundNumber, () -> {
            if (roundNumber == 3) {
                broadcaster.sendRenderCategoriesEvent(gameId, null, true);
            } else {
                broadcaster.sendGetQuestionEvent(gameId);
            }
        });
    }

    private void renderQuestion(QuestionModel question, int questionNumber, int totalQuestions, int roundNumber) {
        log.debug("Render question. GameId {}, questionText: {}", gameId, question.getText());
        renderTopContainerForAdmin(broadcaster.getState(gameId).getUsers().values());
        renderQuestionLayout(question, questionNumber, totalQuestions, roundNumber);
        var type = question.getType().equals(QuestionType.TOP)
                ? AnswerType.TOP
                : roundNumber == 2 ? AnswerType.INPUT : AnswerType.OPTIONS;
        if (question.getType().equals(QuestionType.LINK)) {
            type = AnswerType.MATCH;
        }
        renderAnswersLayout(question, type);
    }

    private void renderAnswersLayout(QuestionModel questionModel, AnswerType answerType) {
        var answerInput = new VerticalLayout();
        answerInput.setPadding(false);
        answerInput.setAlignItems(Alignment.CENTER);

        botContainer.removeAll();
        botContainer.setEnabled(true);

        if (AnswerType.OPTIONS.equals(answerType)) {
            if (questionModel.getType().equals(QuestionType.PHOTO)) {
                if (isAdmin) {
                    answerInput.add(CleverestComponents.userPhotoOptionsInputComponentsCarousel(questionModel));
                } else {
                    answerInput.add(CleverestComponents.userPhotoOptionsInputComponents(questionModel,
                            answerModel -> broadcaster.sendSubmitAnswerEventAndCheckScore(
                                    gameId, SessionWrapper.getLoggedUser(), questionModel, answerModel.getPhotoFilename(), answerModel::isCorrect)));
                }
            } else {
                answerInput.add(CleverestComponents.userTextOptionsInputComponents(questionModel,
                        !isAdmin,
                        answerModel -> broadcaster.sendSubmitAnswerEventAndCheckScore(gameId, SessionWrapper.getLoggedUser(), questionModel, answerModel.getText(), answerModel::isCorrect)));
            }
        } else if (AnswerType.INPUT.equals(answerType) || AnswerType.VOICE_OR_INPUT.equals(answerType)) {
            if (isAdmin) return;
            Button submit = CleverestComponents.submitButton(e -> {
            });

            TextField textField = CleverestComponents.answerInput(event -> {
                submit.setEnabled(!event.getValue().isBlank());
            });
            if (AnswerType.VOICE_OR_INPUT.equals(answerType)) {
                botContainer.add(createQuestionGrade(questionModel));

                VoiceRecognition voiceRecognition = new VoiceRecognition();
                voiceRecognition.addResultListener(event -> {
                    textField.setValue(event.getSpeech());
                });
                answerInput.add(voiceRecognition);
            }
            submit.addClickListener(event -> {
                broadcaster.sendSubmitAnswerEventAndCheckScore(gameId,
                        SessionWrapper.getLoggedUser(),
                        questionModel,
                        textField.getValue(),
                        () -> false);
                textField.setValue("*****");
            });
            answerInput.add(textField, submit);
        } else if (AnswerType.TOP.equals(answerType)) {
            if (isAdmin) return;
            answerInput.add(CleverestComponents.userTopListInputComponents(
                    questionModel,
                    answers -> {
                        broadcaster.sendSubmitAnswerEventAndCheckScore(gameId,
                                SessionWrapper.getLoggedUser(),
                                questionModel,
                                String.join(", ", answers),
                                () -> false);
                    }
            ));
        } else if (AnswerType.MATCH.equals(answerType)) {
            if (isAdmin) return;
            answerInput.add(CleverestComponents.userMatchAnswersComponents(questionModel, pairs -> {
                broadcaster.sendSubmitAnswerEventAndCheckScore(
                        gameId,
                        SessionWrapper.getLoggedUser(),
                        questionModel,
                        pairs.stream().map(pair -> pair.getLeft().getText() + " = " + pair.getRight().getText()).collect(Collectors.joining(", ")),
                        () -> false
                );
            }));
        }
        botContainer.add(answerInput);
    }

    private void renderQuestionLayout(QuestionModel questionModel, int questionNumber, int totalQuestions, int roundNumber) {
        List<String> questionClasses = isAdmin ? List.of(LumoUtility.FontSize.XXXLARGE) : List.of(CleverestComponents.MOBILE_LARGE_FONT);
        String imageHeight = isAdmin ? CleverestComponents.LARGE_IMAGE_HEIGHT : CleverestComponents.MEDIUM_IMAGE_HEIGHT;

        midContainer.removeAll();

        Span questionNumberSpan = new Span();
        questionNumberSpan.setText(String.format("Раунд %d. Вопрос %d/%d", roundNumber, questionNumber, totalQuestions));
        questionNumberSpan.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.AlignSelf.START);

        if (!isAdmin) {
            midContainer.add(createQuestionGrade(questionModel));
        }
        midContainer.add(questionNumberSpan);
        midContainer.add(CleverestComponents.questionLayout(questionModel, questionClasses, imageHeight, isAdmin));
    }

    private VerticalLayout createQuestionGrade(QuestionModel questionModel) {
        return CleverestComponents.questionGradeLayout(scoreVal -> {
            broadcaster.sendQuestionGradedEvent(gameId, questionModel, SessionWrapper.getLoggedUser(), scoreVal);
            CleverestComponents.notification(SessionWrapper.getLoggedUser() + ", спасибо за фидбек!", NotificationVariant.LUMO_CONTRAST);
        });
    }

    private void renderCategoriesTable(UserGameState userToAnswer, Map<String, List<QuestionModel>> data) {
        var categoriesLayout = new VerticalLayout();
        categoriesLayout.setAlignItems(Alignment.START);
        categoriesLayout.setPadding(false);

        midContainer.removeAll();
        midContainer.add(categoriesLayout);

        data.forEach((category, questions) -> {
            HorizontalLayout row = new HorizontalLayout();
            row.addClassNames(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD);
            row.setDefaultVerticalComponentAlignment(Alignment.CENTER);
            row.setAlignItems(Alignment.START);
            row.setMargin(true);

            var categoryName = CleverestComponents.primaryButton(category, event -> {
            });
            categoryName.addClassNames(LumoUtility.FontSize.XXXLARGE);
            categoryName.setEnabled(!questions.stream().allMatch(QuestionModel::isAlreadyAnswered));

            row.add(categoryName);
            questions.forEach(questionModel -> {
                Button openQuestionButton = createCategoryQuestionButton(userToAnswer, questionModel);
                row.add(openQuestionButton);
            });
            categoriesLayout.add(row);
        });
    }

    @SneakyThrows
    private Button createCategoryQuestionButton(UserGameState userToAnswer, QuestionModel question) {
        Button button = CleverestComponents.primaryButton(String.valueOf(question.getPoints()), event -> {
            broadcaster.sendQuestionChoosenEvent(gameId, question, userToAnswer);
            Dialog questionTextDialog = CleverestComponents.openDialog(CleverestComponents.questionLayout(question, List.of(LumoUtility.FontSize.XXXLARGE), "25em", isAdmin), "Вопрос", () -> {
            });
            setAdminAction(() -> {
                questionTextDialog.close();
                AtomicBoolean approved = new AtomicBoolean(false);
                question.setAlreadyAnswered(true);
                showCorrectAnswer(question, Collections.singletonList(userToAnswer), false, 0, true, uName -> {
                    userToAnswer.increaseScore(question.getPoints());
                    approved.set(true);
                    broadcaster.sendUpdatePersonalScoreEvent(gameId);
                }, () -> {
                    if (!approved.get()) {
                        userToAnswer.decreaseScore(question.getPoints());
                    }
                }, () -> broadcaster.sendRenderCategoriesEvent(gameId, question, false));
            });
        });
        button.setEnabled(!question.isAlreadyAnswered());
        button.addClassNames(LumoUtility.FontSize.XXXLARGE);
        return button;
    }

    private void runAdminAction() {
        if (isAdmin) {
            Optional.ofNullable(adminAction).ifPresentOrElse(Runnable::run, () -> log.debug("No admin action to run, gameId: {}", gameId));
            adminAction = null;
        }
    }

    private void setAdminAction(Runnable action) {
        if (isAdmin) {
            this.adminAction = action;
        }
    }

    private void renderUserPersonalScore() {
        UserGameState userState = broadcaster.getState(gameId).getUsers().get(SessionWrapper.getLoggedUser());
        if (userState == null) {
            // This should probably never happen
            log.warn("Not joined user is accessing started Cleverest game");
            return;
        }
        topContainer.removeAll();
        topContainer.add(CleverestComponents.userScoreLayout(SessionWrapper.getLoggedUser(), userState.getColor(), userState.getScore(), CleverestComponents.MOBILE_LARGE_FONT));
    }

    private void showUsersScore(boolean roundOver, int revealScoreAfter, Runnable onCloseAction) {
        var usersScoreLayout = revealScoreAfter == 0
                ? CleverestComponents.usersScoreTableLayout(broadcaster.getState(gameId).getSortedByScoreUsers())
                : new VerticalLayout(CleverestComponents.userInfoLightSpan(
                "Вопросов до таблицы результатов: " + revealScoreAfter, LumoUtility.FontSize.XXXLARGE));

        CleverestComponents.openDialog(usersScoreLayout, "Таблица результатов", () -> {
            if (roundOver) {
                broadcaster.sendNewRoundEvent(gameId);
                return;
            }
            onCloseAction.run();
        });
    }

    private void showCorrectAnswer(QuestionModel question,
                                   Collection<UserGameState> users,
                                   boolean roundOver,
                                   int revealScoreAfter,
                                   boolean approveManually,
                                   Consumer<String> approveAction,
                                   Runnable onCloseAction,
                                   Runnable usersScoreCloseAction) {
        if (users.stream().allMatch(UserGameState::isLastWasCorrect)) {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.CORRECT_ANSWER_AUDIOS.next());
        } else if (users.stream().noneMatch(UserGameState::isLastWasCorrect) && !approveManually) {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.WRONG_ANSWER_AUDIOS.next());
        } else {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.REVEAL_ANSWER_AUDIOS.next());
        }
        VerticalLayout answers = new VerticalLayout();
        answers.setSpacing(true);
        answers.setDefaultHorizontalComponentAlignment(Alignment.START);
        answers.setAlignItems(Alignment.START);
        answers.addClassNames(LumoUtility.FontSize.XXXLARGE);

        // answer span text
        answers.add(CleverestComponents.correctAnswerSpan(question,
                LumoUtility.FontSize.XXXLARGE,
                LumoUtility.FontWeight.SEMIBOLD));
        if (StringUtils.isNotBlank(question.getAnswerDescription())) {
            answers.add(CleverestComponents.correctAnswerDescriptionSpan(question,
                    LumoUtility.FontSize.XXLARGE,
                    LumoUtility.FontWeight.LIGHT));
        }

        users.forEach(userGameState -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setPadding(true);
            row.setWidthFull();
            row.setDefaultVerticalComponentAlignment(Alignment.START);
            row.setAlignItems(Alignment.CENTER);

            if (userGameState.isLastWasCorrect()) {
                row.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY);
            }

            if (!approveManually) {
                row.add(userGameState.isLastWasCorrect() ? CleverestComponents.doneIcon() : CleverestComponents.cancelIcon());
            }

            Span userAnswerSpan = CleverestComponents.userAnswerSpan(userGameState,
                    question.getType(),
                    LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD);
            row.add(userAnswerSpan);
            if (approveManually) {
                int countLimit;
                switch (question.getType()) {
                    case TOP -> countLimit = question.getAnswers().size();
                    case LINK -> countLimit = question.getAnswers().size() / 2;
                    default -> countLimit = 0;
                }
                Button approveButton =
                        CleverestComponents.approveButton(
                                () -> approveAction.accept(userGameState.getUsername()),
                                countLimit);
                row.add(approveButton);
            }
            answers.add(row);
        });

        CleverestComponents.openDialog(answers, "Ответы", () -> {
            onCloseAction.run();
            broadcaster.getState(gameId).updateUserPositions();
            broadcaster.sendUpdatePersonalScoreEvent(gameId);
            broadcaster.sendSaveUserAnswersEvent(gameId, question);
            showUsersScore(roundOver, revealScoreAfter, usersScoreCloseAction);
        });
    }

    private void renderResults() {
        topContainer.removeAll();
        midContainer.removeAll();
        botContainer.removeAll();
        CleverestGameState gameState = broadcaster.getState(gameId);
        if (isAdmin) {
            resultComponent.setState(gameState.getSortedByScoreUsers().values(), gameState.getHistory(), "");
        } else {
            renderUserPersonalScore();
            midContainer.add(CleverestComponents.userInfoLightSpan("Итоговое место: " + gameState.getUsers().get(SessionWrapper.getLoggedUser()).getLastPosition(), CleverestComponents.MOBILE_LARGE_FONT));
            resultComponent.setState(gameState.getSortedByScoreUsers().values(), gameState.getHistory(), SessionWrapper.getLoggedUser());
        }
        midContainer.add(resultComponent);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        log.debug("onAttach: {}", gameId);
        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UserAnsweredEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI(), () -> {
                if (isAdmin) {
                    updateUserAnswerGiven(event.getUserGameState());
                }
                if (event.getUserGameState().getUsername().equals(SessionWrapper.getLoggedUser())) {
                    botContainer.setEnabled(false);
                }
                if (event.getRoundNumber() == 3) {
                    runAdminAction();
                } else {
                    CleverestComponents.notification(event.getUserGameState().getUsername() + " ответил", NotificationVariant.LUMO_CONTRAST);
                }
            });
        }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GetQuestionEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI(),
                    () -> renderQuestion(
                            event.getQuestion(),
                            event.getQuestionNumber(),
                            event.getTotalQuestionsInRound(),
                            event.getRoundNumber()));
        }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.RenderCategoriesEvent.class, event -> {
            QuizUtils.runActionInUi(attachEvent.getUI(), () -> {
                midContainer.removeAll();
                botContainer.removeAll();

                if (isAdmin) {
                    renderTopContainerForAdmin(Collections.singletonList(event.getUserToAnswer()));
                    renderCategoriesTable(event.getUserToAnswer(), event.getData());
                } else {
                    if (SessionWrapper.getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
                        midContainer.add(CleverestComponents.userInfoLightSpan("Время отвечать!", LumoUtility.TextColor.PRIMARY, CleverestComponents.MOBILE_LARGE_FONT));
                    } else {
                        midContainer.add(CleverestComponents.userInfoLightSpan("В ожидании вопроса", LumoUtility.TextColor.SECONDARY, CleverestComponents.MOBILE_LARGE_FONT));
                    }
                }
            });
        }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GameFinishedEvent.class, event -> QuizUtils.runActionInUi(attachEvent.getUI(), this::renderResults)));
        if (isAdmin) {
            subscribeOnAdminOnlyEvents(attachEvent);
        } else {
            subscribeOnPlayerOnlyEvents(attachEvent);
        }
    }

    private void subscribeOnPlayerOnlyEvents(AttachEvent attachEvent) {
        log.debug("Subscribed on player events: {}", SessionWrapper.getLoggedUser());
        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.UpdatePersonalScoreEvent.class,
                event -> QuizUtils.runActionInUi(attachEvent.getUI().getUI(), () -> {
                    log.debug("Updating score from event");
                    renderUserPersonalScore();
                })));
        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.QuestionChoosenEvent.class,
                event -> QuizUtils.runActionInUi(attachEvent.getUI().getUI(),
                        () -> {
                            if (SessionWrapper.getLoggedUser().equals(event.getUserToAnswer().getUsername())) {
                                QuizUtils.runActionInUi(attachEvent.getUI().getUI(),
                                        () -> renderAnswersLayout(event.getQuestion(), AnswerType.VOICE_OR_INPUT));
                            }
                        })));
    }

    private void subscribeOnAdminOnlyEvents(AttachEvent attachEvent) {
        log.debug("Subscribed on admin events: {}", SessionWrapper.getLoggedUser());
        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.AllUsersAnsweredEvent.class, event -> {
            AudioUtils.playStaticSoundAsync(StaticValuesHolder.SUBMIT_ANSWER_SHORT_AUDIOS.next()).thenRun(() -> {
                log.debug("Submit audio finished, run action in ui: {}, {}", attachEvent.getUI(), gameId);
                QuizUtils.runActionInUi(attachEvent.getUI(), () -> {
                    boolean approveManually = event.getCurrentRound() == 2;
                    showCorrectAnswer(event.getQuestion(),
                            broadcaster.getState(gameId).getSortedByResponseTimeUsers().values(),
                            event.isRoundOver(),
                            event.getRevealScoreAfter(),
                            approveManually,
                            uName -> {
                                broadcaster.getState(gameId).getUsers().get(uName).increaseScore();
                                broadcaster.sendUpdatePersonalScoreEvent(gameId);
                            }, () -> {
                            }, () -> broadcaster.sendGetQuestionEvent(gameId));
                });
            });
        }));

        subs.add(broadcaster.subscribe(gameId, CleverestBroadcaster.GetRoundEvent.class,
                event -> QuizUtils.runActionInUi(attachEvent.getUI(), () -> showRoundRules(event.getRoundNumber(), event.getRules()))));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.debug("onDetach: {}", gameId);
        subs.forEach(Registration::remove);
        subs.clear();
    }
}