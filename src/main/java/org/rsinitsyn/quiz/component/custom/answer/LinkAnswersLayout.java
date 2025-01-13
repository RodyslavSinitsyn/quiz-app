package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import org.rsinitsyn.quiz.component.custom.LinkAnswersComponent;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;

import java.util.stream.Collectors;

public class LinkAnswersLayout extends AbstractAnswersLayout {

    private final LinkAnswersComponent component;

    public LinkAnswersLayout(AnswerLayoutRequest request) {
        super(request);
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
                .map(pair -> pair.getLeft().getText() + " = " + pair.getRight().getText())
                .collect(Collectors.toSet());
        fireEvent(new AnswerChosenEvent(userAnswers, areCorrect));
    }
}
