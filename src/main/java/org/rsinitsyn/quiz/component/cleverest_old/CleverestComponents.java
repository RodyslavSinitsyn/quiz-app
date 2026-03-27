package org.rsinitsyn.quiz.component.cleverest_old;

import com.flowingcode.vaadin.addons.carousel.Carousel;
import com.flowingcode.vaadin.addons.carousel.Slide;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.rsinitsyn.quiz.utils.QuizComponents.*;
import static org.rsinitsyn.quiz.utils.QuizUtils.createStreamResourceForPhoto;

public final class CleverestComponents {

    public static final String LARGE_IMAGE_HEIGHT = "30em";
    public static final String MEDIUM_IMAGE_HEIGHT = "17.5em";
    public static final String SMALL_IMAGE_HEIGHT = "12.5em";

    public static final String MOBILE_SMALL_FONT = LumoUtility.FontSize.MEDIUM;
    public static final String MOBILE_MEDIUM_FONT = LumoUtility.FontSize.XLARGE;
    public static final String MOBILE_LARGE_FONT = LumoUtility.FontSize.XXLARGE;

    private CleverestComponents() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static Dialog openDialog(Component component, String headerTitle, Runnable closeAction) {
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

    public static Button emoji(String emoji) {
        final var button = new Button(emoji);
        button.addThemeVariants(ButtonVariant.LUMO_LARGE);
        button.addClassName("emoji");
        return button;
    }

    public static Span questionTextSpan(String text) {
        Span span = new Span();
        span.setText(text);
        span.addClassName("question-text");
        return span;
    }

    public static Span smallTextSpan(String text) {
        Span span = new Span(text);
        span.addClassNames(LumoUtility.FontWeight.LIGHT, MOBILE_SMALL_FONT);
        return span;
    }

    public static HorizontalLayout horizontalLayoutBetween(Component... components) {
        return horizontalLayout(JustifyContentMode.BETWEEN, components);
    }

    public static HorizontalLayout horizontalLayoutCenter(Component... components) {
        return horizontalLayout(JustifyContentMode.CENTER, components);
    }

    public static HorizontalLayout horizontalLayout(JustifyContentMode mode, Component... components) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.setJustifyContentMode(mode);
        layout.add(components);
        return layout;
    }

    public static HorizontalLayout userProfileWithAnswer(UserStateSnapshot userStateSnapshot, QuestionType questionType, String... classes) {
        final var userAnswer = new Span();
        if (questionType.equals(QuestionType.PHOTO)) {
            userAnswer.add(largeAvatar(userStateSnapshot.answerText()));
        } else {
            userAnswer.add(" = [%s]".formatted(userStateSnapshot.answerText()));
        }
        userAnswer.addClassNames(classes);
        final var userProfile = userProfile(userStateSnapshot.profile(), classes);
        userProfile.add(userAnswer);
        return userProfile;
    }

    public static HorizontalLayout userProfileWithScore(UserStateSnapshot snapshot, String... classes) {
        final var userScore = new Span("[%s]".formatted(snapshot.score()));
        userScore.addClassNames(classes);
        userScore.getStyle().set("color", snapshot.color());
        final var userProfile = userProfile(snapshot.profile(), classes);
        userProfile.add(appendTextBorder(userScore));
        return userProfile;
    }

    public static HorizontalLayout userProfile(UserProfile profile, String... classes) {
        return horizontalLayoutCenter(
                profile.avatarResource()
                        .map(data -> (Component) avatar(data, AvatarVariant.LUMO_XLARGE))
                        .orElseGet(VaadinIcon.USER::create),
                appendTextBorder(new Span() {{
                    setText(profile.username());
                    getStyle().set("color", profile.color());
                    addClassNames(classes);
                }}));
    }

    public static Span correctAnswerSpan(QuestionModel questionModel, String... classes) {
        Span span = new Span();
        span.addClassNames(classes);
        span.addClassNames(LumoUtility.TextAlignment.CENTER,
                LumoUtility.Border.ALL,
                LumoUtility.Background.PRIMARY_10,
                LumoUtility.BorderColor.PRIMARY);
        span.setWidthFull();
        span.getStyle().set("white-space", "pre-line");
        if (questionModel.getType().equals(QuestionType.PHOTO)) {
            span.add(image(questionModel.getFirstCorrectAnswer().photoFilename(), MEDIUM_IMAGE_HEIGHT));
        } else {
            span.setText(questionModel.getCorrectAnswersAsText());
        }
        return span;
    }

    public static Span answerDescriptionSpan(String answerDescription, String... classes) {
        Span span = new Span();
        span.addClassNames(classes);
        span.addClassName("answer-description");
        span.setText(answerDescription);
        return span;
    }

    public static Span userInfoLightSpan(String text, String... classes) {
        Span span = new Span();
        span.setText(text);
        span.addClassNames(
                LumoUtility.FontWeight.LIGHT,
                LumoUtility.TextAlignment.CENTER);
        span.addClassNames(classes);
        return span;
    }

    // Form Elements
    public static Notification notification(String text, NotificationVariant variant) {
        Notification notification = Notification.show(text, 1_500, Notification.Position.TOP_STRETCH);
        notification.addThemeVariants(variant);
        return notification;
    }

    public static Div optionComponent(String text,
                                      int maxLength,
                                      ComponentEventListener<ClickEvent<Div>> eventHandler) {
        var option = new Div();
        option.setWidthFull();
        option.setText(text);
        option.addClassNames("quiz-option");
        option.addClickListener(eventHandler);
        return option;
    }

    public static TextField textAnswerInput(HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<TextField, String>> valueChangeHandler) {
        TextField textField = new TextField("Введите ответ");
        textField.setValueChangeMode(ValueChangeMode.EAGER);
        textField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);
        textField.addClassNames(MOBILE_MEDIUM_FONT);
        textField.setWidthFull();
        textField.addValueChangeListener(valueChangeHandler);
        return textField;
    }

    public static Button submitButton(ComponentEventListener<ClickEvent<Button>> clickAction) {
        var submit = primaryButton("Ответить", clickAction);
        submit.setWidthFull();
        submit.setEnabled(false);
        return submit;
    }

    public static Button primaryButton(String text, ComponentEventListener<ClickEvent<Button>> clickAction) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClickListener(clickAction);
        button.addClassNames(MOBILE_MEDIUM_FONT);
        return button;
    }

    public static Button iconButton(Icon icon, ComponentEventListener<ClickEvent<Button>> clickAction) {
        Button button = new Button(icon);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(clickAction);
        return button;
    }

    public static Button approveButton(Runnable clickAction,
                                       int countLimit) {
        final var button = iconButton(VaadinIcon.CHECK.create(), event -> {
        });
        button.addClickListener(event -> {
            if (countLimit > 0) {
                String currText = event.getSource().getElement().getText();
                var countValue = currText.isBlank()
                        ? 1
                        : Integer.parseInt(button.getText()) + 1;
                if (countValue <= countLimit) {
                    button.setText(String.valueOf(countValue));
                    clickAction.run();
                }
            } else {
                clickAction.run();
                button.setEnabled(false);
            }
            event.getSource().getParent().ifPresent(p ->
                    p.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY));
        });
        return button;
    }

    // Icons
    public static Icon doneIcon() {
        Icon icon = VaadinIcon.CHECK.create();
        icon.getElement().getThemeList().add("badge success");
        return icon;
    }

    public static Icon cancelIcon() {
        Icon icon = VaadinIcon.CLOSE_SMALL.create();
        icon.getElement().getThemeList().add("badge error");
        return icon;
    }

    public static Icon userCheckIcon() {
        return VaadinIcon.USER_CHECK.create();
    }

    // Big Business Layouts
    public static VerticalLayout usersScoreTableLayout(Map<String, UserGameState> users) {
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

            row.add(userProfileWithScore(userGameState.snapshot(), LumoUtility.FontSize.XXXLARGE));

            layout.add(row);
        });
        return layout;
    }

    public static VerticalLayout questionGradeLayout(Consumer<Integer> eventHandler) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.setWidthFull();
        layout.setAlignItems(Alignment.CENTER);
        layout.add(userInfoLightSpan("Оцените сложность вопроса", MOBILE_SMALL_FONT));

        // TODO: Not working with new Vaadin, find replacement
//        StarsRating rating = new StarsRating(0, 5, true);
//        rating.addValueChangeListener(event -> eventHandler.accept(event.getValue()));
//        layout.add(rating);

        return layout;
    }

    public static Image image(String filename) {
        return image(filename, null);
    }

    public static Image image(String filename, String height) {
        Image image = new Image();
        image.setSrc(createStreamResourceForPhoto(filename));
        image.addClassName("quiz-photo");
        if (height != null) {
            image.setMaxHeight(height);
        }
        return image;
    }

    public static VerticalLayout manualPhotoCarousel(List<String> photoFilenames) {
        var slides = photoFilenames.stream()
                .map(CleverestComponents::image)
                .map(Slide::new)
                .toArray(Slide[]::new);

        Carousel carousel = new Carousel(slides)
                .withStartPosition(photoFilenames.size() - 1)
                .withoutNavigation();
        carousel.addClassName("quiz-carousel");
        carousel.setHeight(MEDIUM_IMAGE_HEIGHT);

        final var prev = iconButton(VaadinIcon.ARROW_CIRCLE_LEFT_O.create(), event -> carousel.movePrev());
        final var next = iconButton(VaadinIcon.ARROW_CIRCLE_RIGHT_O.create(), event -> carousel.moveNext());
        return new VerticalLayout(carousel, horizontalLayoutCenter(prev, next));
    }
}
