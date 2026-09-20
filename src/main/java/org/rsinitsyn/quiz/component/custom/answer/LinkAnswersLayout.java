package org.rsinitsyn.quiz.component.custom.answer;

import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.component.custom.LinkAnswersComponent;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.entity.UserAnswerDetails;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.answer.AnswerResult;

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
        var pairs = component.getPairs();
        var correctCount = (int) pairs.stream()
                .filter(pair -> pair.getLeft().number() == pair.getRight().number())
                .count();
        var answerStatus = AnswerStatus.answerStatus(correctCount, pairs.size());
        return AnswerGivenEvent.builder()
                .answerDetails(UserAnswerDetails.fromLink(
                        pairs.stream().map(pair -> pair.getLeft().text() + " = " + pair.getRight().text()).toList(),
                        pairs.stream().map(MutablePair::getLeft).map(QuestionModel.AnswerModel::id).toList(),
                        pairs.stream().map(MutablePair::getRight).map(QuestionModel.AnswerModel::id).toList()
                ))
                .result(new AnswerResult(answerStatus, pairs.size(), correctCount))
                .build();
    }
}
