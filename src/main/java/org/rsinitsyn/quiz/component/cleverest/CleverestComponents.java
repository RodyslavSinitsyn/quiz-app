package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.component.сustom.ColorPicker;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.utils.StaticValuesHolder;

@UtilityClass
public class CleverestComponents {

    public Dialog openDialog(Component component, String headerTitle, Runnable action) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(headerTitle);
        dialog.add(component);
        dialog.setCloseOnOutsideClick(true);
        dialog.addDialogCloseActionListener(event -> {
            event.getSource().close();
            action.run();
        });
        dialog.open();
        return dialog;
    }

    public Span questionTextSpan(String text, String... classes) {
        Span span = new Span();
        span.setText(text);
        span.addClassNames(LumoUtility.LineHeight.XSMALL, LumoUtility.TextAlignment.CENTER);
        span.addClassNames(classes);
        span.getStyle().set("white-space", "pre-line");
        span.setWidthFull();
        return span;
    }

    public Span userScore(String username, String ustTxtColor, int score, String... classes) {
        Span userScore = new Span(String.valueOf(score));
        userScore.addClassNames(classes);

        Span result = new Span(userNameSpan(username, ustTxtColor, classes), delimiterSpan(classes), userScore);
        result.addClassNames(LumoUtility.TextAlignment.CENTER);
        return result;
    }

    public Span userAnswer(String username, String usrTxtColor, String answer, boolean correct, String... classes) {
        Span userScore = new Span(String.valueOf(answer));
        userScore.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        userScore.addClassNames(classes);

        Span result = new Span(userNameSpan(username, usrTxtColor, classes), delimiterSpan(classes), userScore);
        result.addClassNames(LumoUtility.TextAlignment.CENTER);
        if (correct) {
            result.addClassNames(LumoUtility.Background.SUCCESS_10);
        }
        return result;
    }

    public Span userNameSpan(String username, String textColor, String... classes) {
        return new Span() {{
            setText(username);
            getStyle().set("text-shadow", StaticValuesHolder.BLACK_FONT_BORDER);
            getStyle().set("color", textColor);
            addClassNames(classes);
        }};
    }

    public Button optionButton(String text, Runnable action) {
        Button button = new Button();
        button.setText(text);
        button.addThemeVariants(ButtonVariant.LUMO_LARGE);
        button.addClassNames(LumoUtility.BorderColor.PRIMARY, LumoUtility.Border.ALL);
        button.setWidthFull();
        button.addClassNames(LumoUtility.FontSize.XXLARGE);
        button.addClickListener(event -> {
            action.run();
        });
        return button;
    }

    public TextField answerInput(HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<TextField, String>> eventHandler) {
        TextField textField = new TextField("Введите ответ");
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
        textField.addClassNames(LumoUtility.FontSize.LARGE);
        textField.setWidthFull();
        textField.addValueChangeListener(eventHandler);
        return textField;
    }

    public Button primaryButton(String text, ComponentEventListener<ClickEvent<Button>> clickAction) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClickListener(clickAction);
        button.addClassNames(LumoUtility.FontSize.LARGE);
        return button;
    }

    public Button approveButton(Runnable clickAction,
                                ButtonVariant... variants) {
        Button button = new Button();
        button.setIcon(VaadinIcon.CHECK.create());
        button.setDisableOnClick(true);
        button.addClickListener(event -> {
            event.getSource().getParent().ifPresent(p -> p.addClassNames(LumoUtility.Background.SUCCESS_10));
            clickAction.run();
        });
        button.addThemeVariants(variants);
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

    public ConfirmDialog confirmDialog(String username, ComponentEventListener<ConfirmDialog.ConfirmEvent> eventHandler) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setCloseOnEsc(true);
        dialog.setRejectable(false);
        dialog.setCancelable(true);
        dialog.setCancelText("Назад");
        dialog.setConfirmText("Играть");
        dialog.addConfirmListener(eventHandler);

        VerticalLayout layout = new VerticalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);
        layout.setSpacing(true);

        Span playerName = new Span(username);
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.addValueChangeListener(event -> {

        });

        layout.add(playerName, colorPicker);

        return dialog;
    }

    public Icon doneIcon() {
        Icon icon = VaadinIcon.CHECK.create();
        icon.getElement().getThemeList().add("badge success");
        return icon;
    }

    public Icon cancelIcon() {
        Icon icon = VaadinIcon.CLOSE_SMALL.create();
        icon.getElement().getThemeList().add("badge error");
        return icon;
    }


    private Span delimiterSpan(String... classes) {
        Span span = new Span();
        span.setText(": ");
        span.addClassNames(classes);
        return span;
    }
}
