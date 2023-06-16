package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;

public class LinkQuestionsComponent extends HorizontalLayout {

    private final List<MutablePair<LinkItemDto, LinkItemDto>> resultPairs = new ArrayList<>();
    private final int desiredPairsSize;
    private final Map<String, Boolean> colors = new HashMap<>();

    {
        colors.put(LumoUtility.Background.PRIMARY_10, false);
        colors.put(LumoUtility.Background.SUCCESS_50, false);
        colors.put(LumoUtility.Background.SUCCESS_10, false);
        colors.put(LumoUtility.Background.ERROR_50, false);
        colors.put(LumoUtility.Background.ERROR_10, false);
        colors.put(LumoUtility.Background.CONTRAST_30, false);
    }

    private LinkItemDto currLeft = null;
    private LinkItemDto currRight = null;
    private Boolean leftSelectedFirst = null;

    public LinkQuestionsComponent(QuestionModel questionModel) {
        this.desiredPairsSize = (int) questionModel.getAnswers().stream().filter(AnswerModel::isCorrect).count();

        setPadding(false);
        setWidthFull();

        List<AnswerModel> left = questionModel.getShuffledAnswers().stream().filter(AnswerModel::isCorrect).toList();
        List<AnswerModel> right = questionModel.getShuffledAnswers().stream().filter(am -> !am.isCorrect()).toList();

        VerticalLayout leftSide = matchSideLayout(left, true);
        VerticalLayout rightSide = matchSideLayout(right, false);

        add(leftSide, rightSide);
    }

    public List<MutablePair<AnswerModel, AnswerModel>> getPairs() {
        return resultPairs.stream()
                .map(pair -> MutablePair.of(pair.getLeft().getAnswer(), pair.getRight().getAnswer()))
                .toList();
    }

    private VerticalLayout matchSideLayout(List<AnswerModel> answers,
                                           boolean isLeft) {
        VerticalLayout side = new VerticalLayout();
        side.setMargin(false);
        side.setPadding(false);
        side.setSpacing(true);
        answers.forEach(answerModel -> {
            Component component = matchAnswerComponent(answerModel, isLeft, event -> {
                if (currLeft == null || currRight == null) return;

                if (currLeftLinked() && currRightLinked()) {
                    var linkedByLeft = getPairByLeftComponent();
                    var linkedByRight = getPairByRightComponent();
                    if (linkedByLeft != linkedByRight) {
                        var leftSideColor = linkedByLeft.getLeft().getColor();
                        var rightSideColor = linkedByRight.getRight().getColor();
                        linkedByLeft.getRight().getComponent().removeClassName(leftSideColor);
                        linkedByRight.getLeft().getComponent().removeClassName(rightSideColor);
                        String dominantColor = null;
                        if (leftSelectedFirst) {
                            dominantColor = leftSideColor;
                            linkedByRight.getRight().getComponent().removeClassName(rightSideColor);
                            colors.put(rightSideColor, false);
                        } else {
                            dominantColor = rightSideColor;
                            linkedByLeft.getLeft().getComponent().removeClassName(leftSideColor);
                            colors.put(leftSideColor, false);
                        }
                        resultPairs.remove(linkedByLeft);
                        resultPairs.remove(linkedByRight);

                        createAndAddNewPair(dominantColor);
                    }
                } else if (currLeftLinked() && !currRightLinked()) {
                    var linkedByLeft = getPairByLeftComponent();
                    var leftSideColor = linkedByLeft.getLeft().getColor();
                    linkedByLeft.getRight().getComponent().removeClassName(leftSideColor);
                    markRightComponentWithColor(leftSideColor);
                    linkedByLeft.setRight(currRight);
                } else if (!currLeftLinked() && currRightLinked()) {
                    var linkedByRight = getPairByRightComponent();
                    var rightSideColor = linkedByRight.getRight().getColor();
                    linkedByRight.getLeft().getComponent().removeClassName(rightSideColor);
                    markLeftComponentWithColor(rightSideColor);
                    linkedByRight.setLeft(currLeft);
                } else {
                    createAndAddNewPair(getFreeColor());
                }
                cleanupState();
                getEventBus().fireEvent(new PairLinkedEvent(this, desiredPairsSize == resultPairs.size()));
            });
            side.add(component);
        });
        return side;
    }

    private void createAndAddNewPair(String color) {
        markLeftComponentWithColor(color);
        markRightComponentWithColor(color);
        colors.put(color, true);

        var newPair = MutablePair.of(currLeft, currRight);
        resultPairs.add(newPair);
    }

    private void markLeftComponentWithColor(String colorClass) {
        currLeft.setColor(colorClass);
        if (!currLeft.getComponent().hasClassName(colorClass)) {
            currLeft.getComponent().addClassNames(colorClass);
        }
    }

    private void markRightComponentWithColor(String colorClass) {
        currRight.setColor(colorClass);
        if (!currRight.getComponent().hasClassName(colorClass)) {
            currRight.getComponent().addClassNames(colorClass);
        }
    }

    private void markComponentAsSelected(Component component, boolean bolded) {
        if (bolded) {
            component.removeClassName(LumoUtility.TextColor.PRIMARY);
            component.addClassNames(LumoUtility.TextColor.BODY);
        } else {
            component.removeClassName(LumoUtility.TextColor.BODY);
            component.addClassNames(LumoUtility.TextColor.PRIMARY);
        }
    }

    private void cleanupState() {
        markComponentAsSelected(currLeft.getComponent(), false);
        markComponentAsSelected(currRight.getComponent(), false);

        currLeft = null;
        currRight = null;

        leftSelectedFirst = null;
    }

    private MutablePair<LinkItemDto, LinkItemDto> getPairByLeftComponent() {
        return resultPairs.stream()
                .filter(pair -> pair.getLeft().getComponent().equals(currLeft.getComponent()))
                .findFirst().orElse(null);
    }

    private MutablePair<LinkItemDto, LinkItemDto> getPairByRightComponent() {
        return resultPairs.stream()
                .filter(pair -> pair.getRight().getComponent().equals(currRight.getComponent()))
                .findFirst().orElse(null);
    }

    private boolean currLeftLinked() {
        return getPairByLeftComponent() != null;
    }

    private boolean currRightLinked() {
        return getPairByRightComponent() != null;
    }

    private Component matchAnswerComponent(AnswerModel answerModel,
                                           boolean isLeft,
                                           ComponentEventListener<ClickEvent<Div>> eventHandler) {
        return CleverestComponents.optionComponent(
                answerModel.getText(),
                10,
                event -> {
                    if (leftSelectedFirst == null) {
                        leftSelectedFirst = isLeft;
                    }
                    if (currLeft == null && isLeft) {
                        currLeft = new LinkItemDto(event.getSource(), answerModel, null, isLeft);
                    } else if (currRight == null && !isLeft) {
                        currRight = new LinkItemDto(event.getSource(), answerModel, null, isLeft);
                    }
                    updateSideRow(isLeft ? currLeft : currRight, isLeft, event.getSource(), answerModel);
                    eventHandler.onComponentEvent(event);
                }
        );
    }

    private void updateSideRow(LinkItemDto currDto,
                               boolean isLeft,
                               Component selectedComponent,
                               AnswerModel answerModel) {
        markComponentAsSelected(currDto.getComponent(), false);
        currDto.setComponent(selectedComponent);
        markComponentAsSelected(currDto.getComponent(), true);

        currDto.setLeft(isLeft);
        currDto.setAnswer(answerModel);
    }


    private String getFreeColor() {
        return colors.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public static class PairLinkedEvent extends ComponentEvent<LinkQuestionsComponent> {
        @Getter
        private boolean done;

        public PairLinkedEvent(LinkQuestionsComponent source,
                               boolean done) {
            super(source, true);
            this.done = done;
        }
    }

    public Registration addPairLinkedEventListener(ComponentEventListener<PairLinkedEvent> listener) {
        return getEventBus().addListener(PairLinkedEvent.class, listener);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class LinkItemDto {
        private Component component;
        private AnswerModel answer;
        private String color;
        private boolean isLeft;
    }
}
