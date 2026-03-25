package org.rsinitsyn.quiz.component.custom.question;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.custom.AudioPlayer;
import org.rsinitsyn.quiz.component.custom.answer.AbstractAnswersLayout;
import org.rsinitsyn.quiz.component.custom.answer.AbstractAnswersLayout.AnswerGivenEvent;
import org.rsinitsyn.quiz.component.custom.event.StubEvent;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.ArrayList;
import java.util.List;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;
import static org.rsinitsyn.quiz.component.custom.answer.AnswerLayoutsFactory.createAnswerLayout;

@Slf4j
public class BaseQuestionLayout extends VerticalLayout {

    protected final QuestionModel questionModel;
    protected final boolean host;
    protected final String imageHeight;
    protected final List<String> textContentClasses;

    @Getter
    private AbstractAnswersLayout answersLayout;

    private final List<Registration> subscriptions = new ArrayList<>();

    public BaseQuestionLayout(QuestionLayoutRequest request) {
        this.questionModel = request.question();
        this.host = request.host();
        this.imageHeight = request.imageHeight();
        this.textContentClasses = request.textClasses();
        configureStyling();
        renderComponents(request);
    }

    private void configureStyling() {
        setSpacing(false);
        setPadding(false);
        addClassNames(LumoUtility.Margin.Top.MEDIUM);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

    private void renderComponents(QuestionLayoutRequest request) {
        renderCategory();
        renderQuestionText();
        renderImage();
        renderAudio();
        renderAnswersLayout(request);
    }

    protected void renderImage() {
        questionModel.photoFilename()
                .ifPresent(filename -> add(image(filename, imageHeight)));
    }

    protected void renderCategory() {
        add(smallTextSpan(questionModel.getCategoryName()));
    }

    protected void renderQuestionText() {
        add(getQuestionTextElement());
    }

    protected Span getQuestionTextElement() {
        return questionTextSpan(questionModel.getText(), textContentClasses.toArray(new String[]{}));
    }

    protected void renderAudio() {
        questionModel.audioFilename().ifPresent(filename -> {
            Button playAudioButton = new Button("Слушать", VaadinIcon.PLAY_CIRCLE.create());
            playAudioButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST,
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SMALL);
            playAudioButton.setEnabled(host);
            playAudioButton.addClickListener(event -> {
                AudioUtils.playSoundAsync(filename);
            });

            add(playAudioButton);

            //  TODO: Play audio on each device
            AudioPlayer audioPlayer = new AudioPlayer(QuizUtils.createStreamResourceForAudio(filename));
            add(audioPlayer);
        });
    }

    private void renderAnswersLayout(QuestionLayoutRequest request) {
        answersLayout = createAnswerLayout(AnswerLayoutRequest.builder()
                .question(questionModel)
                .hintsState(request.hintsState())
                .build());
        answersLayout.setEnabled(!host);
        add(answersLayout);
    }

    @Getter
    public static class QuestionAnsweredEvent extends StubEvent {
        private final QuestionModel question;
        private final AnswerGivenEvent answerGivenEvent;

        public QuestionAnsweredEvent(final QuestionModel question,
                                     final AnswerGivenEvent answerGivenEvent) {
            this.question = question;
            this.answerGivenEvent = answerGivenEvent;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (answersLayout != null) {
            final var registration = answersLayout.addListener(
                    AnswerGivenEvent.class,
                    event -> {
                        fireEvent(new QuestionAnsweredEvent(questionModel, event));
                    });
            subscriptions.add(registration);
        }
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
