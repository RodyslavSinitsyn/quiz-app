package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import org.rsinitsyn.quiz.component.custom.LinkAnswersComponent;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LinkAnswersLayout extends AbstractAnswersLayout {

    private final LinkAnswersComponent component;

    public LinkAnswersLayout(QuestionModel question) {
        super(question);
        this.component = new LinkAnswersComponent(question);
    }

    @Override
    protected void renderAnswers() {
        component.addPairLinkedEventListener(linkedEvent -> submitButton.setEnabled(linkedEvent.isDone()));
        add(component);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        var pairs = component.getPairs(); // TODO: For now true if get all the matches
        boolean areCorrect = true;
        for (var pair : pairs) {
            if (pair.getLeft().getNumber() != pair.getRight().getNumber()) {
                areCorrect = false;
                break;
            }
        }
        var userAnswers = pairs.stream()
                .flatMap(pair -> Stream.of(pair.getLeft(), pair.getRight()))
                .collect(Collectors.toSet());
        fireEvent(new AnswerChosenEvent<>(this, userAnswers, areCorrect));
    }

    @Override
    protected boolean isSubmitButtonEnabled() {
        return false;
    }
}
