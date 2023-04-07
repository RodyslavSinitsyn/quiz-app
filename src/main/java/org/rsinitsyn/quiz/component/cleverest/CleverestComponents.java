package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@UtilityClass
public class CleverestComponents {

    public Dialog defaultDialog(Component component, String headerTitle, Runnable action) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(headerTitle);
        dialog.add(component);
        dialog.setCloseOnOutsideClick(true);
        dialog.addDialogCloseActionListener(event -> {
            event.getSource().close();
            action.run();
        });
        return dialog;
    }

    public Span userScore(String username, int score, String fontSize) {
        Span usernameSpan = new Span(username);
        usernameSpan.addClassNames(LumoUtility.FontWeight.THIN, fontSize);

        Span userScore = new Span(String.valueOf(score));
        userScore.addClassNames(
                LumoUtility.FontWeight.SEMIBOLD,
                fontSize,
                LumoUtility.TextColor.SUCCESS);

        return new Span(usernameSpan, delimiterSpan(fontSize), userScore);
    }

    public Span userAnswer(String username, String answer, String fontSize) {
        Span usernameSpan = new Span(username);
        usernameSpan.addClassNames(LumoUtility.FontWeight.THIN, fontSize);

        Span userScore = new Span(String.valueOf(answer));
        userScore.addClassNames(LumoUtility.FontWeight.BOLD,
                fontSize,
                LumoUtility.TextColor.SECONDARY);

        return new Span(usernameSpan, delimiterSpan(fontSize), userScore);
    }

    public Button optionButton(String text, Runnable action) {
        Button button = new Button();
        button.setText(text);
        button.addThemeVariants(ButtonVariant.LUMO_LARGE);
        button.setWidthFull();
        button.setHeight("auto");
        button.addClassNames(LumoUtility.FontSize.XXLARGE);
        button.addClickListener(event -> {
            action.run();
        });
        return button;
    }

    public TextField defaultInput(HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<TextField, String>> eventHandler) {
        TextField textField = new TextField("Введите ответ");
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
        textField.setWidthFull();
        textField.addClassNames(LumoUtility.FontSize.XXXLARGE);
        textField.addValueChangeListener(eventHandler);
        return textField;
    }

    public Button submitButton() {
        Button button = new Button("Ответить");
        button.addThemeVariants(ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_PRIMARY);
        button.setWidthFull();
        button.addClassNames(LumoUtility.FontSize.XXLARGE);
        return button;
    }

    public Span correctAnswerSpan(QuizQuestionModel questionModel, String... classes) {
        Span span = new Span();
        span.addClassNames(classes);
        span.addClassNames(LumoUtility.TextAlignment.CENTER);
        span.setWidthFull();
        span.setText(questionModel.getFirstCorrectAnswer().getText());
        return span;
    }

    private Span delimiterSpan(String fontSize) {
        Span span = new Span();
        span.setText(": ");
        span.addClassNames(fontSize, LumoUtility.FontWeight.THIN);
        return span;
    }
}
