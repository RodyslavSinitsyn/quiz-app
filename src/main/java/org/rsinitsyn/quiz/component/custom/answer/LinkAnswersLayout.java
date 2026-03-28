package org.rsinitsyn.quiz.component.custom.answer;

import org.rsinitsyn.quiz.component.custom.LinkAnswersComponent;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

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
    protected AnswerGivenEvent createAnswerGivenEvent() {
        var pairs = component.getPairs(); // TODO: For now true if get all the matches
        var correctCount = 0;
        for (var pair : pairs) {
            if (pair.getLeft().number() == pair.getRight().number()) {
                correctCount++;
                break;
            }
        }
        var answerStatus = AnswerStatus.answerStatus(correctCount, pairs.size());
        return AnswerGivenEvent.builder()
                .answers(pairs.stream()
                        .map(pair -> pair.getLeft().text() + " = " + pair.getRight().text())
                        .collect(Collectors.toSet()))
                .result(new AnswerResult(answerStatus, pairs.size(), correctCount))
                .build();
    }
}
