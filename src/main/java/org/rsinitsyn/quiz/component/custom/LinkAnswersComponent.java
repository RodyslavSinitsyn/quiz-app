package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.Getter;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;

import java.util.*;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.*;

public class LinkAnswersComponent extends HorizontalLayout {

    private final List<MutablePair<LinkItemDto, LinkItemDto>> resultPairs = new ArrayList<>();
    private final int desiredPairsSize;
    private final Map<String, Boolean> colors = new LinkedHashMap<>();

    {
        colors.put(LumoUtility.Background.PRIMARY_50, false);
        colors.put(LumoUtility.Background.SUCCESS_50, false);
        colors.put(LumoUtility.Background.ERROR_50, false);
        colors.put(LumoUtility.Background.CONTRAST_30, false);
        colors.put(LumoUtility.Background.PRIMARY_10, false);
        colors.put(LumoUtility.Background.SUCCESS_10, false);
        colors.put(LumoUtility.Background.ERROR_10, false);
    }

    private LinkItemDto currLeft = null;
    private LinkItemDto currRight = null;
    private Boolean leftSelectedFirst = null;

    public LinkAnswersComponent(QuestionModel questionModel) {
        this.desiredPairsSize = (int) questionModel.getAnswers().stream().filter(AnswerModel::correct).count();

        setPadding(false);
        setWidthFull();

        final var left = questionModel.getShuffledAnswers().stream().filter(AnswerModel::correct).toList();
        final var right = questionModel.getShuffledAnswers().stream().filter(am -> !am.correct()).toList();

        final var leftSide = matchSideLayout(left, true);
        final var rightSide = matchSideLayout(right, false);

        setFlexGrow(1, leftSide);
        setFlexGrow(1, rightSide);

        add(leftSide, rightSide);
    }

    public List<MutablePair<AnswerModel, AnswerModel>> getPairs() {
        return resultPairs.stream()
                .map(pair -> MutablePair.of(pair.getLeft().answer(), pair.getRight().answer()))
                .toList();
    }

    private VerticalLayout matchSideLayout(List<AnswerModel> answers, boolean isLeft) {
        final var side = new VerticalLayout();
        side.setMargin(false);
        side.setPadding(false);
        side.setSpacing(true);
        side.setWidthFull();
        answers.forEach(answer -> side.add(buildSelectableComponent(answer, isLeft, event -> {
            if (currLeft == null || currRight == null) return;
            handlePairEvent();
            cleanupState();
            getEventBus().fireEvent(new PairLinkedEvent(this, desiredPairsSize == resultPairs.size()));
        })));
        return side;
    }

    private void handlePairEvent() {
        if (currLeftLinked() && currRightLinked()) {
            handleBothLinked();
        } else if (currLeftLinked()) {
            handleLeftLinkedOnly();
        } else if (currRightLinked()) {
            handleRightLinkedOnly();
        } else {
            createAndAddNewPair(getFreeColor());
        }
    }

    private void handleBothLinked() {
        final var linkedByLeft = getPairByLeftComponent();
        final var linkedByRight = getPairByRightComponent();
        if (linkedByLeft == linkedByRight) return;

        final var leftSideColor = linkedByLeft.getLeft().color();
        final var rightSideColor = linkedByRight.getRight().color();
        linkedByLeft.getRight().component().removeClassName(leftSideColor);
        linkedByRight.getLeft().component().removeClassName(rightSideColor);

        final String dominantColor;
        if (leftSelectedFirst) {
            dominantColor = leftSideColor;
            linkedByRight.getRight().component().removeClassName(rightSideColor);
            colors.put(rightSideColor, false);
        } else {
            dominantColor = rightSideColor;
            linkedByLeft.getLeft().component().removeClassName(leftSideColor);
            colors.put(leftSideColor, false);
        }
        resultPairs.remove(linkedByLeft);
        resultPairs.remove(linkedByRight);
        createAndAddNewPair(dominantColor);
    }

    private void handleLeftLinkedOnly() {
        final var linkedByLeft = getPairByLeftComponent();
        final var color = linkedByLeft.getLeft().color();
        linkedByLeft.getRight().component().removeClassName(color);
        markRightComponentWithColor(color);
        linkedByLeft.setRight(currRight);
    }

    private void handleRightLinkedOnly() {
        final var linkedByRight = getPairByRightComponent();
        final var color = linkedByRight.getRight().color();
        linkedByRight.getLeft().component().removeClassName(color);
        markLeftComponentWithColor(color);
        linkedByRight.setLeft(currLeft);
    }

    private void createAndAddNewPair(String color) {
        markLeftComponentWithColor(color);
        markRightComponentWithColor(color);
        colors.put(color, true);
        resultPairs.add(MutablePair.of(currLeft, currRight));
    }

    private void markLeftComponentWithColor(String colorClass) {
        currLeft = currLeft.withColor(colorClass);
        applyColorClass(currLeft.component(), colorClass);
    }

    private void markRightComponentWithColor(String colorClass) {
        currRight = currRight.withColor(colorClass);
        applyColorClass(currRight.component(), colorClass);
    }

    private void applyColorClass(Component component, String colorClass) {
        if (!component.hasClassName(colorClass)) {
            component.addClassNames(colorClass);
        }
    }

    private void markComponentAsSelected(Component component, boolean selected) {
        if (selected) {
            component.removeClassName(LumoUtility.TextColor.PRIMARY);
            component.addClassNames(LumoUtility.TextColor.BODY);
        } else {
            component.removeClassName(LumoUtility.TextColor.BODY);
            component.addClassNames(LumoUtility.TextColor.PRIMARY);
        }
    }

    private void cleanupState() {
        markComponentAsSelected(currLeft.component(), false);
        markComponentAsSelected(currRight.component(), false);
        currLeft = null;
        currRight = null;
        leftSelectedFirst = null;
    }

    private MutablePair<LinkItemDto, LinkItemDto> getPairByLeftComponent() {
        return resultPairs.stream()
                .filter(pair -> pair.getLeft().component().equals(currLeft.component()))
                .findFirst().orElse(null);
    }

    private MutablePair<LinkItemDto, LinkItemDto> getPairByRightComponent() {
        return resultPairs.stream()
                .filter(pair -> pair.getRight().component().equals(currRight.component()))
                .findFirst().orElse(null);
    }

    private boolean currLeftLinked() {
        return getPairByLeftComponent() != null;
    }

    private boolean currRightLinked() {
        return getPairByRightComponent() != null;
    }

    private Component buildSelectableComponent(AnswerModel answer,
                                               boolean isLeft,
                                               ComponentEventListener<ClickEvent<Component>> eventHandler) {
        return switch (answer.type()) {
            case TEXT -> createTextComponent(answer, isLeft, eventHandler);
            case PHOTO -> createImageComponent(answer, isLeft, eventHandler);
            case AUDIO -> createAudioComponent(answer, isLeft, eventHandler);
        };
    }

    private Component createImageComponent(AnswerModel answer,
                                           boolean isLeft,
                                           ComponentEventListener<ClickEvent<Component>> eventHandler) {
        return imageOptionComponent(answer.photoFilename(), "5em",
                event -> onAnswerSelected(event, answer, isLeft, eventHandler));
    }

    private Component createAudioComponent(AnswerModel answer,
                                           boolean isLeft,
                                           ComponentEventListener<ClickEvent<Component>> eventHandler) {
        return audioOptionComponent(answer.audioFilename(),
                event -> onAnswerSelected(event, answer, isLeft, eventHandler));
    }

    private Component createTextComponent(AnswerModel answer,
                                          boolean isLeft,
                                          ComponentEventListener<ClickEvent<Component>> eventHandler) {
        return optionComponent(answer.text(), 10,
                event -> onAnswerSelected(event, answer, isLeft, eventHandler));
    }

    private void onAnswerSelected(ClickEvent<? extends Component> event,
                                  AnswerModel answer,
                                  boolean isLeft,
                                  ComponentEventListener<ClickEvent<Component>> eventHandler) {
        if (leftSelectedFirst == null) {
            leftSelectedFirst = isLeft;
        }
        final var dto = new LinkItemDto(event.getSource(), answer, null);
        if (isLeft && currLeft == null) {
            currLeft = dto;
        } else if (!isLeft && currRight == null) {
            currRight = dto;
        }
        updateSideSlot(isLeft, event.getSource(), answer);
        eventHandler.onComponentEvent((ClickEvent<Component>) event);
    }

    private void updateSideSlot(boolean isLeft, Component selectedComponent, AnswerModel answer) {
        final var curr = isLeft ? currLeft : currRight;
        markComponentAsSelected(curr.component(), false);

        final var updated = new LinkItemDto(selectedComponent, answer, curr.color());
        if (isLeft) {
            currLeft = updated;
        } else {
            currRight = updated;
        }
        markComponentAsSelected(selectedComponent, true);
    }

    private String getFreeColor() {
        return colors.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    public static class PairLinkedEvent extends ComponentEvent<LinkAnswersComponent> {
        @Getter
        private final boolean done;

        public PairLinkedEvent(LinkAnswersComponent source, boolean done) {
            super(source, true);
            this.done = done;
        }
    }

    public Registration addPairLinkedEventListener(ComponentEventListener<PairLinkedEvent> listener) {
        return getEventBus().addListener(PairLinkedEvent.class, listener);
    }

    private record LinkItemDto(Component component, AnswerModel answer, String color) {
        LinkItemDto withColor(String newColor) {
            return new LinkItemDto(component, answer, newColor);
        }

        LinkItemDto withComponent(Component newComponent) {
            return new LinkItemDto(newComponent, answer, color);
        }
    }
}
