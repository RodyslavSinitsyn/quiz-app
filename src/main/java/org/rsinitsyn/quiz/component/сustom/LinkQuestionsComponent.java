package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Span;
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
    private final Map<String, Boolean> colors = new HashMap<>();
    {
        colors.put(LumoUtility.Background.PRIMARY_10, false);
        colors.put(LumoUtility.Background.SUCCESS_10, false);
        colors.put(LumoUtility.Background.ERROR_10, false);
        colors.put(LumoUtility.Background.CONTRAST_10, false);
    }
    private final int desiredPairsSize;

    private LinkItemDto currLeft = null;
    private LinkItemDto currRight = null;
    private Boolean leftSelectedFirst = null;
    private String currColor;

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
            Span span = matchAnswerSpan(answerModel, isLeft, event -> {
                if (currLeft == null || currRight == null) return;

                if (currLeftLinked() && currRightLinked()) {
                    var linkedByLeft = getPairByLeftSpan();
                    var linkedByRight = getPairByRightSpan();
                    if (linkedByLeft != linkedByRight) {
                        var leftSideColor = linkedByLeft.getLeft().getColor();
                        var rightSideColor = linkedByRight.getRight().getColor();
                        linkedByLeft.getRight().getSpan().removeClassName(leftSideColor);
                        linkedByRight.getLeft().getSpan().removeClassName(rightSideColor);
                        String dominantColor = null;
                        if (leftSelectedFirst) {
                            dominantColor = leftSideColor;
                            linkedByRight.getRight().getSpan().removeClassName(rightSideColor);
                            colors.put(rightSideColor, false);
                        } else {
                            dominantColor = rightSideColor;
                            linkedByLeft.getLeft().getSpan().removeClassName(leftSideColor);
                            colors.put(leftSideColor, false);
                        }
                        resultPairs.remove(linkedByLeft);
                        resultPairs.remove(linkedByRight);

                        createAndAddNewPair(dominantColor);
                    }
                } else if (currLeftLinked() && !currRightLinked()) {
                    var linkedByLeft = getPairByLeftSpan();
                    var leftSideColor = linkedByLeft.getLeft().getColor();
                    linkedByLeft.getRight().getSpan().removeClassName(leftSideColor);
                    markRightSpanWithColor(leftSideColor);
                    linkedByLeft.setRight(currRight);
                } else if (!currLeftLinked() && currRightLinked()) {
                    var linkedByRight = getPairByRightSpan();
                    var rightSideColor = linkedByRight.getRight().getColor();
                    linkedByRight.getLeft().getSpan().removeClassName(rightSideColor);
                    markLeftSpanWithColor(rightSideColor);
                    linkedByRight.setLeft(currLeft);
                } else {
                    createAndAddNewPair(getFreeColor());
                }
                cleanupState();
                getEventBus().fireEvent(new PairLinkedEvent(this, desiredPairsSize == resultPairs.size()));
            });
            side.add(span);
        });
        return side;
    }

    private void createAndAddNewPair(String color) {
        markLeftSpanWithColor(color);
        markRightSpanWithColor(color);
        colors.put(color, true);

        var newPair = MutablePair.of(currLeft, currRight);
        resultPairs.add(newPair);
    }

    private void markLeftSpanWithColor(String colorClass) {
        currLeft.setColor(colorClass);
        if (!currLeft.getSpan().hasClassName(colorClass)) {
            currLeft.getSpan().addClassNames(colorClass);
        }
    }

    private void markRightSpanWithColor(String colorClass) {
        currRight.setColor(colorClass);
        if (!currRight.getSpan().hasClassName(colorClass)) {
            currRight.getSpan().addClassNames(colorClass);
        }
    }

    private void markSpanWithTextColor(Span span, boolean bolded) {
        if (bolded) {
            span.removeClassName(LumoUtility.TextColor.PRIMARY);
            span.addClassNames(LumoUtility.TextColor.BODY);
        } else {
            span.removeClassName(LumoUtility.TextColor.BODY);
            span.addClassNames(LumoUtility.TextColor.PRIMARY);
        }
    }

    private void cleanupState() {
        markSpanWithTextColor(currLeft.getSpan(), false);
        markSpanWithTextColor(currRight.getSpan(), false);

        currLeft = null;
        currRight = null;

        leftSelectedFirst = null;
    }

    private MutablePair<LinkItemDto, LinkItemDto> getPairByLeftSpan() {
        return resultPairs.stream()
                .filter(pair -> pair.getLeft().getSpan().equals(currLeft.getSpan()))
                .findFirst().orElse(null);
    }

    private MutablePair<LinkItemDto, LinkItemDto> getPairByRightSpan() {
        return resultPairs.stream()
                .filter(pair -> pair.getRight().getSpan().equals(currRight.getSpan()))
                .findFirst().orElse(null);
    }

    private boolean currLeftLinked() {
        return getPairByLeftSpan() != null;
    }

    private boolean currRightLinked() {
        return getPairByRightSpan() != null;
    }

    private Span matchAnswerSpan(AnswerModel answerModel,
                                 boolean isLeft,
                                 ComponentEventListener<ClickEvent<Span>> eventHandler) {
        Span span = new Span(answerModel.getText());
        span.setWidthFull();
        span.addClassNames(
                answerModel.getText().length() <= 10
                        ? CleverestComponents.MOBILE_MEDIUM_FONT
                        : CleverestComponents.MOBILE_SMALL_FONT,
                LumoUtility.TextAlignment.CENTER,
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.BorderRadius.MEDIUM);
        span.addClickListener(event -> {
            if (leftSelectedFirst == null) {
                leftSelectedFirst = isLeft;
            }
            if (currLeft == null && isLeft) {
                currLeft = new LinkItemDto(event.getSource(), answerModel, null, isLeft);
            } else if (currRight == null && !isLeft) {
                currRight = new LinkItemDto(event.getSource(), answerModel, null, isLeft);
            }
            setSomeFields(isLeft ? currLeft : currRight, isLeft, event.getSource(), answerModel);
            eventHandler.onComponentEvent(event);
        });
        return span;
    }

    private void setSomeFields(LinkItemDto currDto,
                               boolean isLeft,
                               Span clickedSpan,
                               AnswerModel answerModel) {
        // unselect old span and select new one
        markSpanWithTextColor(currDto.getSpan(), false);
        currDto.setSpan(clickedSpan);
        markSpanWithTextColor(currDto.getSpan(), true);

        // set props
        currDto.setLeft(isLeft);
        currDto.setAnswer(answerModel);
    }

    private void assignValuesAndColorToCurr(LinkItemDto currDto,
                                            boolean isLeft,
                                            Span clickedSpan,
                                            AnswerModel answerModel) {
        String colorToRemove = null;
        if (isLeft && currLeftLinked()) {
            if (currColor == null) {
                currColor = getPairByLeftSpan().getLeft().getColor();
            } else {
                colorToRemove = getPairByLeftSpan().getLeft().getColor();
            }
        } else if (!isLeft && currRightLinked()) {
            if (currColor == null) {
                currColor = getPairByRightSpan().getRight().getColor();
            } else {
                colorToRemove = getPairByRightSpan().getRight().getColor();
            }
        }
        currColor = currColor == null ? getFreeColor() : currColor;
        colorToRemove = colorToRemove != null ? colorToRemove : currColor;

        currDto.getSpan().removeClassName(colorToRemove);
        clickedSpan.addClassNames(currColor);

        currDto.setColor(currColor);
        currDto.setSpan(clickedSpan);
        currDto.setAnswer(answerModel);
        currDto.setLeft(isLeft);
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
        private Span span;
        private AnswerModel answer;
        private String color;
        private boolean isLeft;
    }
}
