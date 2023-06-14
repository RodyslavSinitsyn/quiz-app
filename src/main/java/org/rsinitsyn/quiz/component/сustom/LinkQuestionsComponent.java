package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
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
    private LinkItemDto currLeft = null;
    private LinkItemDto currRight = null;
    private String currColor;

    private final Map<String, Boolean> colors = new HashMap<>();

    {
        colors.put(LumoUtility.Background.PRIMARY_10, false);
        colors.put(LumoUtility.Background.SUCCESS_10, false);
        colors.put(LumoUtility.Background.ERROR_10, false);
        colors.put(LumoUtility.Background.CONTRAST_10, false);
    }

    public LinkQuestionsComponent(QuestionModel questionModel) {
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
                if (currLeft != null && currRight != null) {
                    if (currLeftLinked() && currRightLinked()) {
                        var linkedByLeft = getPairByLeftSpan();
                        var linkedByRight = getPairByRightSpan();
                        if (linkedByLeft == linkedByRight) {
                            Notification.show("Click on same");
                        } else {
                            linkedByLeft.getRight().getSpan().removeClassName(linkedByLeft.getRight().getColor());
                            colors.put(linkedByLeft.getRight().getColor(), false);
                            linkedByRight.getLeft().getSpan().removeClassName(linkedByRight.getLeft().getColor());
                            colors.put(linkedByLeft.getRight().getColor(), false);

                            resultPairs.remove(linkedByLeft);
                            resultPairs.remove(linkedByRight);

                            var newPair = MutablePair.of(currLeft, currRight);
                            resultPairs.add(newPair);
                        }
                    } else if (currLeftLinked() && !currRightLinked()) {
                        var linkedByLeft = getPairByLeftSpan();
                        linkedByLeft.getRight().getSpan().removeClassName(linkedByLeft.getRight().getColor());
                        colors.put(linkedByLeft.getRight().getColor(), false);
                        linkedByLeft.setRight(currRight);
                    } else if (!currLeftLinked() && currRightLinked()) {
                        var linkedByRight = getPairByRightSpan();
                        linkedByRight.getLeft().getSpan().removeClassName(linkedByRight.getLeft().getColor());
                        colors.put(linkedByRight.getLeft().getColor(), false);
                        linkedByRight.setLeft(currLeft);
                    } else {
                        var newPair = MutablePair.of(currLeft, currRight);
                        resultPairs.add(newPair);
                    }
                    cleanupState();
                }
            });
            side.add(span);
        });
        return side;
    }

    private MutablePair<Span, AnswerModel> makeCopy(MutablePair<Span, AnswerModel> pair) {
        return MutablePair.of(pair.getKey(), pair.getValue());
    }

    private void cleanupState() {
        colors.put(currColor, true);
        currColor = null;
        currLeft = null;
        currRight = null;

        System.out.println("-------");
        System.out.println(colors);
        System.out.println("pairs size: " + resultPairs.size());
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
                CleverestComponents.MOBILE_MEDIUM_FONT,
                LumoUtility.TextAlignment.CENTER,
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.BorderRadius.MEDIUM);
        span.addClickListener(event -> {
            if (currLeft == null && isLeft) {
                currLeft = new LinkItemDto(event.getSource(), answerModel, null, isLeft);
            } else if (currRight == null && !isLeft) {
                currRight = new LinkItemDto(event.getSource(), answerModel, null, isLeft);
            }
            assignValuesAndColorToCurr(isLeft ? currLeft : currRight, isLeft, event.getSource(), answerModel);
            eventHandler.onComponentEvent(event);
        });
        return span;
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

    public static class LinkingCompletedEvent extends ComponentEvent<LinkQuestionsComponent> {
        @Getter
        private List<MutablePair<AnswerModel, AnswerModel>> result;

        public LinkingCompletedEvent(LinkQuestionsComponent source,
                                     List<MutablePair<AnswerModel, AnswerModel>> result) {
            super(source, true);
            this.result = result;
        }
    }

    public Registration addLinkingCompletedEventListener(ComponentEventListener<LinkingCompletedEvent> listener) {
        return getEventBus().addListener(LinkingCompletedEvent.class, listener);
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
