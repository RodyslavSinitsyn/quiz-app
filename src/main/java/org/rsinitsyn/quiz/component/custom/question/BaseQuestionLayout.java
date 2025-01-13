package org.rsinitsyn.quiz.component.custom.question;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.component.custom.answer.AbstractAnswersLayout;
import org.rsinitsyn.quiz.component.custom.answer.AnswerLayoutsFactory;
import org.rsinitsyn.quiz.component.custom.event.StubEvent;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BaseQuestionLayout extends VerticalLayout {

    protected final QuestionModel questionModel;
    protected final List<String> textContentClasses = List.of(LumoUtility.FontSize.XXXLARGE);
    protected final String imageHeight = "25em";
    protected final boolean isAdmin = false;

    @Getter
    private AbstractAnswersLayout answersLayout;

    private List<Registration> subscriptions = new ArrayList<>();

    public BaseQuestionLayout(QuestionModel questionModel) {
        this.questionModel = questionModel;

        configureStyles();
        renderComponents();
    }

    private void configureStyles() {
        setSpacing(false);
        setPadding(false);
        addClassNames(LumoUtility.Margin.Top.MEDIUM);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
    }

    private void renderComponents() {
        renderCategory();
        renderImage();
        renderQuestionText();
        renderAudio();
        renderAnswersLayout();
    }

    protected void renderImage() {
        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getPhotoFilename()));
            image.setMaxHeight(imageHeight);
            if (!isAdmin) image.setWidthFull();
            image.getStyle().set("object-fit", "cover");
            image.getStyle().set("object-position", "center center");

            add(image);
        }
    }

    protected void renderCategory() {
        Span categorySpan = new Span(questionModel.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.LIGHT, CleverestComponents.MOBILE_SMALL_FONT);

        add(categorySpan);
    }

    protected void renderQuestionText() {
        add(getQuestionTextElement());
    }

    protected Span getQuestionTextElement() {
        return CleverestComponents.questionTextSpan(
                questionModel.getText(),
                textContentClasses.toArray(new String[]{}));
    }

    protected void renderAudio() {
        if (StringUtils.isNotEmpty(questionModel.getAudioFilename())) {
            Button playAudioButton = new Button("Слушать", VaadinIcon.PLAY_CIRCLE.create());
            playAudioButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST,
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SMALL);
            playAudioButton.setEnabled(isAdmin);
            playAudioButton.addClickListener(event -> {
                AudioUtils.playSoundAsync(questionModel.getAudioFilename());
            });

            add(playAudioButton);
            // TODO Play audio on each device
//            AudioPlayer audioPlayer = new AudioPlayer(QuizUtils.createStreamResourceForAudio(questionModel.getAudioFilename()));
//            layout.add(audioPlayer);
        }
    }

    private void renderAnswersLayout() {
        answersLayout = AnswerLayoutsFactory.get(questionModel);
        add(answersLayout);
    }

    @Getter
    public static class QuestionAnsweredEvent extends StubEvent {
        private final AbstractAnswersLayout.AnswerChosenEvent answerChosenEvent;

        public QuestionAnsweredEvent(AbstractAnswersLayout.AnswerChosenEvent answerChosenEvent) {
            this.answerChosenEvent = answerChosenEvent;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        subscriptions.add(answersLayout.addListener(AbstractAnswersLayout.AnswerChosenEvent.class, event -> {
            fireEvent(new QuestionAnsweredEvent(event));
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
