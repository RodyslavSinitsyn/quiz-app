package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;
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

    public HorizontalLayout userScore(String username, String ustTxtColor, int score, String... classes) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);
        layout.setWidthFull();

        Span userScore = new Span(String.valueOf(score));
        userScore.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        userScore.addClassNames(classes);

        layout.add(new Span(userNameSpan(username, ustTxtColor, classes), delimiterSpan(classes)));
        layout.add(userScore);

        return layout;
    }

    public Span userAnswer(String username, String usrTxtColor, String answer, boolean correct, String... classes) {
        Span userTextAnswer = new Span(String.valueOf(answer));
        userTextAnswer.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        userTextAnswer.addClassNames(classes);

        Span result = new Span(userNameSpan(username, usrTxtColor, classes), delimiterSpan(classes), userTextAnswer);
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
        button.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderColor.SUCCESS);
        button.addClickListener(event -> {
            event.getSource().getParent().ifPresent(p -> p.addClassNames(LumoUtility.Background.SUCCESS_10));
            clickAction.run();
        });
        button.addThemeVariants(variants);
        return button;
    }

    public Span correctAnswerSpan(QuestionModel questionModel, String... classes) {
        Span span = new Span();
        span.addClassNames(classes);
        span.addClassNames(LumoUtility.TextAlignment.CENTER,
                LumoUtility.Border.BOTTOM,
                LumoUtility.BorderColor.SUCCESS);
        span.setWidthFull();
        span.setText(questionModel.getFirstCorrectAnswer().getText());
        return span;
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

    public VerticalLayout questionLayout(QuestionModel questionModel,
                                         List<String> textContentClasses,
                                         String imageHeight) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getPhotoFilename()));
            image.setMaxHeight(imageHeight);
            layout.add(image);
        }

        Span categorySpan = new Span(questionModel.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.FontSize.SMALL);
        layout.add(categorySpan);

        Span textContent = CleverestComponents.questionTextSpan(
                questionModel.getText(),
                textContentClasses.toArray(new String[]{}));
        layout.add(textContent);

        if (StringUtils.isNotEmpty(questionModel.getAudioFilename())) {
            Button playAudioButton = new Button("Слушать");
            playAudioButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST,
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SMALL);
            playAudioButton.setIcon(VaadinIcon.PLAY_CIRCLE.create());
            playAudioButton.addClickListener(event -> {
                AudioUtils.playSoundAsync(questionModel.getAudioFilename());
            });
            layout.add(playAudioButton);
        }
        return layout;
    }

    public Span userWaitSpan(String text, String... classes) {
        Span span = new Span();
        span.setText(text);
        span.addClassNames(LumoUtility.FontSize.XXLARGE,
                LumoUtility.FontWeight.LIGHT,
                LumoUtility.TextAlignment.CENTER,
                LumoUtility.Border.BOTTOM,
                LumoUtility.BorderColor.PRIMARY);
        span.addClassNames(classes);
        return span;
    }

    public Notification notification(String text, NotificationVariant variant) {
        Notification notification = Notification.show(text, 1_500, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(variant);
        return notification;
    }

    private Span delimiterSpan(String... classes) {
        Span span = new Span();
        span.setText(": ");
        span.addClassNames(classes);
        return span;
    }
}
