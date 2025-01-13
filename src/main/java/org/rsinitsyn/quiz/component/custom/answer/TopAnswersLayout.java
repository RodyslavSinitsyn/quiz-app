package org.rsinitsyn.quiz.component.custom.answer;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.model.AnswerLayoutRequest;

import java.util.Set;
import java.util.stream.Collectors;

import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.MOBILE_MEDIUM_FONT;

public class TopAnswersLayout extends AbstractAnswersLayout {

    private int topSize = 0;

    private final VerticalLayout topListLayout = new VerticalLayout();

    public TopAnswersLayout(AnswerLayoutRequest question) {
        super(question);
    }

    @Override
    protected void submitHandler(ClickEvent<Button> event) {
        Set<String> answerModels = topListLayout.getChildren()
                .map(component -> component.getElement().getText())
                .collect(Collectors.toSet());
        fireEvent(new AnswerChosenEvent(answerModels, false, true));
    }

    @Override
    protected void renderAnswers() {
        this.topSize = question.getAnswers().size();

        topListLayout.setSpacing(false);
        topListLayout.setPadding(false);

        Button addToListButton = new Button(VaadinIcon.PLUS_CIRCLE.create());
        TextField textField = CleverestComponents.answerInput(event -> {
            addToListButton.setEnabled(!event.getValue().isBlank());
            submitButton.setEnabled(topListLayout.getChildren().count() == topSize);
        });
        configureAddToListButton(addToListButton, textField);

        HorizontalLayout userInput = new HorizontalLayout(textField, addToListButton);
        userInput.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        userInput.setAlignItems(FlexComponent.Alignment.END);
        userInput.setWidthFull();

        add(userInput);
        add(topListLayout);
    }

    private void configureAddToListButton(Button addToListButton, TextField textField) {
        addToListButton.addClickShortcut(Key.ENTER);
        addToListButton.addClickListener(event -> {
            Button topListItem = new Button(textField.getValue());
            topListItem.addClassNames(MOBILE_MEDIUM_FONT);
            topListItem.setWidthFull();
            topListItem.addClickListener(e -> {
                topListLayout.remove(e.getSource());
                textField.setEnabled(topListLayout.getChildren().count() != topSize);
                submitButton.setEnabled(topListLayout.getChildren().count() == topSize);
            });
            topListLayout.add(topListItem);
            textField.setValue("");
            textField.setEnabled(topListLayout.getChildren().count() != topSize);
        });
        addToListButton.setEnabled(false);
    }
}
