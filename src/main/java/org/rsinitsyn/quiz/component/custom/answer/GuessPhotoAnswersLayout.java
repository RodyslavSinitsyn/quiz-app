package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel.HintModel;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.*;

public class GuessPhotoAnswersLayout extends AbstractAnswersLayout {

    private final List<HintModel> photoHints;
    private final Div scoreAndRevealContainer = new Div();
    private final VerticalLayout imagesLayout = new VerticalLayout();
    private final Button revealButton = iconButton(VaadinIcon.EYE.create(), event -> revealNext());
    private final TextField answerField = textAnswerInput(event -> {
    });

    private int currentHintIndex = 0;

    public GuessPhotoAnswersLayout(final AnswerLayoutRequest request) {
        super(request);
        this.photoHints = question.getHints().stream()
                .filter(HintModel::photoType)
                .sorted(Comparator.comparingInt(HintModel::number))
                .toList();
    }

    @Override
    protected void renderAnswers() {
        updateImages();
        updateScore();
        answerField.addValueChangeListener(event -> submitButton.setEnabled(!event.getValue().isBlank()));
        add(scoreAndRevealContainer, imagesLayout, answerField);
    }

    private void updateImages() {
        imagesLayout.removeAll();
        final var photoFilenames = photoHints.stream()
                .takeWhile(h -> h.number() <= currentHintIndex)
                .map(HintModel::photoFilename)
                .toList();
        imagesLayout.add(manualPhotoCarousel(photoFilenames));
    }

    private void revealNext() {
        if (currentHintIndex < photoHints.size() - 1) {
            currentHintIndex++;
            updateImages();
            updateScore();
        }

        if (currentHintIndex == photoHints.size() - 1) {
            revealButton.setEnabled(false);
        }
    }

    private void updateScore() {
        scoreAndRevealContainer.removeAll();
        final var score = photoHints.size() - currentHintIndex;
        scoreAndRevealContainer.add(horizontalLayoutBetween(
                smallTextSpan("Очков за правильный ответ: " + score),
                revealButton));
    }

    @Override
    protected AnswerGivenEvent createAnswerGivenEvent() {
        return new AnswerGivenEvent(Set.of(answerField.getValue()), false, true);
    }
}
