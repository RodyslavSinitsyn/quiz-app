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
import de.jfancy.StarsRating;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

@UtilityClass
public class CleverestComponents {

    public static final String MOBILE_SMALL_FONT = LumoUtility.FontSize.MEDIUM;
    public static final String MOBILE_MEDIUM_FONT = LumoUtility.FontSize.XLARGE;
    public static final String MOBILE_LARGE_FONT = LumoUtility.FontSize.XXLARGE;

    public Dialog openDialog(Component component, String headerTitle, Runnable closeAction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(headerTitle);
        dialog.add(component);
        dialog.setCloseOnOutsideClick(true);
        dialog.addDialogCloseActionListener(event -> {
            event.getSource().close();
            closeAction.run();
        });
        dialog.open();
        return dialog;
    }

    public Span questionTextSpan(String text, String... classes) {
        Span span = new Span();
        span.setText(text);
        span.addClassNames(
                LumoUtility.FontWeight.SEMIBOLD,
                LumoUtility.LineHeight.XSMALL,
                LumoUtility.TextAlignment.CENTER,
                LumoUtility.Whitespace.PRE_LINE);
        span.addClassNames(classes);
        span.setWidthFull();
        return span;
    }

    public Span userAnswerSpan(UserGameState userGameState, String... classes) {
        Span userTextAnswer = new Span(String.valueOf(userGameState.getLastAnswerText()));
        userTextAnswer.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        userTextAnswer.addClassNames(classes);

        return new Span(
                userNameSpan(userGameState.getUsername(), userGameState.getColor(), classes),
                delimiterSpan(classes),
                userTextAnswer);
    }

    public Span userNameSpan(String username, String textColor, String... classes) {
        return QuizComponents.appendTextBorder(new Span() {{
            setText(username);
            getStyle().set("color", textColor);
            addClassNames(classes);
        }});
    }

    public Span correctAnswerSpan(QuestionModel questionModel, String... classes) {
        Span span = new Span();
        span.addClassNames(classes);
        span.addClassNames(LumoUtility.TextAlignment.CENTER,
                LumoUtility.Border.ALL,
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderColor.PRIMARY);
        span.setWidthFull();
        span.setText(questionModel.getFirstCorrectAnswer().getText());
        return span;
    }

    public Span userInfoLightSpan(String text, String... classes) {
        Span span = new Span();
        span.setText(text);
        span.addClassNames(
                LumoUtility.FontWeight.LIGHT,
                LumoUtility.TextAlignment.CENTER);
        span.addClassNames(classes);
        return span;
    }


    private Span delimiterSpan(String... classes) {
        Span span = new Span();
        span.setText(": ");
        span.addClassNames(classes);
        return span;
    }

    // Form Elements
    public Notification notification(String text, NotificationVariant variant) {
        Notification notification = Notification.show(text, 1_500, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(variant);
        return notification;
    }

    public Button optionButton(String text, Runnable action) {
        Button button = new Button();
        button.setWidthFull();
        button.setText(text);
        button.addThemeVariants(ButtonVariant.LUMO_LARGE);
        button.addClassNames(
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Border.ALL,
                text.length() > 20 ? MOBILE_MEDIUM_FONT : MOBILE_LARGE_FONT);
        button.addClickListener(event -> {
            action.run();
        });
        return button;
    }

    public TextField answerInput(HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<TextField, String>> eventHandler) {
        TextField textField = new TextField("Введите ответ");
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
        textField.addClassNames(MOBILE_MEDIUM_FONT);
        textField.setWidthFull();
        textField.addValueChangeListener(eventHandler);
        return textField;
    }

    public Button primaryButton(String text, ComponentEventListener<ClickEvent<Button>> clickAction) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClickListener(clickAction);
        button.addClassNames(MOBILE_MEDIUM_FONT);
        return button;
    }

    public Button approveButton(Runnable clickAction,
                                ButtonVariant... variants) {
        Button button = new Button();
        button.setIcon(VaadinIcon.CHECK.create());
        button.setDisableOnClick(true);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> {
            event.getSource().getParent().ifPresent(p ->
                    p.addClassNames(
                            LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY));
            clickAction.run();
        });
        button.addThemeVariants(variants);
        return button;
    }

    // Icons
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

    public Icon userCheckIcon() {
        return VaadinIcon.USER_CHECK.create();
    }

    // Big Business Layouts
    public VerticalLayout questionLayout(QuestionModel questionModel,
                                         List<String> textContentClasses,
                                         String imageHeight,
                                         boolean allowedPlaySound) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getPhotoFilename()));
            image.setMaxHeight(imageHeight);
            image.addClassNames(LumoUtility.BorderColor.PRIMARY, LumoUtility.Border.ALL);
            layout.add(image);
        }

        Span categorySpan = new Span(questionModel.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.LIGHT, MOBILE_SMALL_FONT);
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
            if (allowedPlaySound) {
                playAudioButton.addClickListener(event -> {
                    AudioUtils.playSoundAsync(questionModel.getAudioFilename());
                });
            }
            layout.add(playAudioButton);
        }
        return layout;
    }

    public VerticalLayout usersScoreTableLayout(Map<String, UserGameState> users) {
        var layout = new VerticalLayout();
        users.forEach((username, userGameState) -> {
            HorizontalLayout row = new HorizontalLayout();

            Span positionSpan = new Span();
            positionSpan.addClassNames(LumoUtility.FontSize.XXXLARGE,
                    LumoUtility.FontWeight.SEMIBOLD);
            if (userGameState.getLastPosition() == 1) {
                positionSpan.add(VaadinIcon.ACADEMY_CAP.create());
            } else if (userGameState.getLastPosition() == users.size()) {
                positionSpan.add(VaadinIcon.GLASS.create());
            } else {
                positionSpan.setText(userGameState.getLastPosition() + ".");
            }
            row.add(positionSpan);

            row.add(CleverestComponents.userScoreLayout(username,
                    userGameState.getColor(),
                    userGameState.getScore(),
                    LumoUtility.FontSize.XXXLARGE));

            layout.add(row);
        });
        return layout;
    }

    public HorizontalLayout userScoreLayout(String username, String ustTxtColor, int score, String... classes) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.STRETCH);
        layout.addClassNames(LumoUtility.Border.BOTTOM,
                LumoUtility.FontWeight.SEMIBOLD);
        layout.getStyle().set("border-color", ustTxtColor);
        layout.setWidthFull();

        Span userScore = new Span(String.valueOf(score));
        userScore.addClassNames(classes);
        userScore.getStyle().set("color", ustTxtColor);

        layout.add(userNameSpan(username, ustTxtColor, classes),
                QuizComponents.appendTextBorder(userScore));

        return layout;
    }

    public VerticalLayout questionGradeLayout(Consumer<Integer> eventHandler) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setWidthFull();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(userInfoLightSpan("Оцените сложность вопроса", MOBILE_SMALL_FONT));

        StarsRating rating = new StarsRating(0, 5, true);
        rating.addValueChangeListener(event -> eventHandler.accept(event.getValue()));
        layout.add(rating);

        return layout;
    }
}
