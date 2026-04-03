package org.rsinitsyn.quiz.component.cleverest;

import com.flowingcode.vaadin.addons.carousel.Carousel;
import com.flowingcode.vaadin.addons.carousel.Slide;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.ContextMenu;
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
import com.vaadin.flow.component.popover.Popover;
import com.vaadin.flow.component.popover.PopoverVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.custom.Emoji;
import org.rsinitsyn.quiz.component.theme.ThemePreset;
import org.rsinitsyn.quiz.entity.AnswerStatus;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.cleverest.ManualApprove;
import org.rsinitsyn.quiz.model.cleverest.UserProfile;
import org.rsinitsyn.quiz.model.cleverest.UserStateSnapshot;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.rsinitsyn.quiz.utils.QuizComponents.*;

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

    public static Span emojiSmall(String emoji) {
        final var span = new Span(emoji);
        span.addClassName("hoover");
        return span;
    }

    public static Button emojiBig(String emoji) {
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
        span.addClassNames(MOBILE_MEDIUM_FONT);
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
            userAnswer.add(userStateSnapshot.answerText());
        }
        userAnswer.addClassNames(classes);
        final var userProfile = userProfile(userStateSnapshot.profile(), classes);
        userProfile.add(userAnswer);
        return userProfile;
    }

    public static HorizontalLayout userProfileWithScore(UserStateSnapshot snapshot, String... classes) {
        final var userScore = new Span("[%s]".formatted(snapshot.score()));
        userScore.addClassNames(classes);
        userScore.addClassName("score-label");
        userScore.getStyle().set("color", snapshot.color());
        final var userProfile = userProfile(snapshot.profile(), classes);
        userProfile.add(appendTextBorder(userScore));
        return userProfile;
    }

    public static HorizontalLayout userProfile(UserProfile profile, String... classes) {
        return horizontalLayoutCenter(
                userPhoto(profile),
                appendTextBorder(new Span() {{
                    setText(profile.username());
                    getStyle().set("color", profile.color());
                    addClassNames(classes);
                }}));
    }

    public static Component userPhoto(UserProfile profile) {
        return profile.photoFilename()
                .map(url -> (Component) avatarByUrl(url, AvatarVariant.LUMO_XLARGE))
                .orElseGet(VaadinIcon.USER::create);
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

    public static Notification notification(String text,
                                            NotificationVariant variant,
                                            Notification.Position position) {
        Notification notification = Notification.show(text, 1_500, position);
        notification.addThemeVariants(variant);
        return notification;
    }

    public static Notification chatNotification(UserProfile profile, String text) {
        Notification notification = new Notification();
        notification.setDuration(2_000);
        notification.setPosition(Notification.Position.TOP_END);
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        final var userProfile = userProfile(profile);
        userProfile.add(new Span("%s".formatted(text)));
        notification.add(userProfile);
        notification.open();
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
        TextField textField = new TextField("Напиши ответ");
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
                                       int clicksLimit,
                                       int pointsPerClick) {
        final var button = iconButton(VaadinIcon.CHECK.create(), event -> {
        });
        button.addClickListener(event -> {
            if (clicksLimit > 0) {
                String currText = event.getSource().getElement().getText();
                var countValue = currText.isBlank()
                        ? pointsPerClick
                        : Integer.parseInt(button.getText()) + pointsPerClick;
                if (countValue <= clicksLimit) {
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
        return iconWithBadge(VaadinIcon.CHECK.create(), "success");
    }

    public static Icon cancelIcon() {
        return iconWithBadge(VaadinIcon.CLOSE_SMALL.create(), "error");
    }

    public static Icon iconWithBadge(Icon icon, String badge) {
        icon.getElement().getThemeList().add("badge %s".formatted(badge));
        return icon;
    }

    public static Icon userCheckIcon() {
        return VaadinIcon.USER_CHECK.create();
    }

    // Big Business Layouts
    public static VerticalLayout usersScoreTableLayout(List<UserStateSnapshot> users,
                                                       Map<String, List<AnswerStatus>> lastAnswers) {
        var layout = new VerticalLayout(JustifyContentMode.START);
        users.forEach(userStateSnapshot -> {
            Span emojiSpan;
            if (userStateSnapshot.position() == 1) {
                emojiSpan = emojiSmall(Emoji.randomGood().value);
            } else if (userStateSnapshot.position() == users.size()) {
                emojiSpan = emojiSmall(Emoji.randomBad().value);
            } else {
                emojiSpan = emojiSmall(Emoji.randomMid().value);
            }
            final var icons = lastAnswers.get(userStateSnapshot.username()).stream()
                    .map(CleverestComponents::getIconFromAnswer)
                    .toArray(Icon[]::new);
            final var row = horizontalLayoutBetween(emojiSpan,
                    smallTextSpan(String.valueOf(userStateSnapshot.position())),
                    userProfileWithScore(userStateSnapshot));
            row.add(icons);
            layout.addClassNames(MOBILE_MEDIUM_FONT);
            layout.add(row);
        });
        return layout;
    }

    public static Image image(String filename) {
        return image(filename, null);
    }

    public static Image image(String filename, String height) {
        Image image = new Image();
        image.setSrc("/quiz-images/%s".formatted(filename));
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

    public static VerticalLayout userAnswersLayout(QuestionModel question,
                                                   Collection<UserStateSnapshot> users,
                                                   Optional<ManualApprove> manualApprove) {
        VerticalLayout answersLayout = new VerticalLayout();

        answersLayout.add(correctAnswerSpan(question));
        question.answerDescription().ifPresent(answerDescription ->
                answersLayout.add(answerDescriptionSpan(answerDescription, MOBILE_SMALL_FONT)));
        users.forEach(userStateSnapshot -> {
            final var userProfileWithAnswer = userProfileWithAnswer(userStateSnapshot,
                    question.getType());
            if (userStateSnapshot.correct()) {
                userProfileWithAnswer.addClassNames(LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY);
            }
            if (manualApprove.isEmpty()) {
                userProfileWithAnswer.add(getIconFromAnswer(userStateSnapshot.answerStatus()));
            }
            if (manualApprove.isPresent()) {
                final var approve = manualApprove.orElseThrow();
                Button approveButton = approveButton(
                        () -> approve.action().accept(userStateSnapshot.username()),
                        approve.clickLimit(),
                        approve.pointsPerClick());
                userProfileWithAnswer.add(approveButton);
            }
            answersLayout.add(userProfileWithAnswer);
        });
        answersLayout.addClassNames(MOBILE_MEDIUM_FONT);
        return answersLayout;
    }

    public static Icon getIconFromAnswer(AnswerStatus status) {
        return switch (status) {
            case CORRECT -> doneIcon();
            case WRONG -> cancelIcon();
            case PARTIAL -> iconWithBadge(VaadinIcon.STAR_HALF_RIGHT_O.create(), "secondary");
            case UNKNOWN -> iconWithBadge(VaadinIcon.QUESTION.create(), "warning");
        };
    }

    public static HorizontalLayout themeColor(ThemePreset preset) {
        final var circle = new Div();
        circle.setWidth("10px");
        circle.setHeight("10px");
        circle.getStyle()
                .set("border-radius", "50%")
                .set("background-color", preset.color())
                .set("border", "1px solid var(--lumo-contrast-20pct)");
        final var label = new Span(preset.name());
        return horizontalLayout(JustifyContentMode.START, circle, label);
    }


    public static Button soundButton(Runnable task) {
        final var soundButton = new Button(Emoji.SOUND.value);
        final var contextMenu = new ContextMenu(soundButton);
        contextMenu.setOpenOnClick(true);
        final var layout = horizontalLayoutCenter();
        Stream.of(Emoji.GUITAR.value, Emoji.LAUGH.value, Emoji.EXPLODE.value)
                .map(CleverestComponents::emojiBig)
                .forEach(emoji -> {
                    layout.add(emoji);
                    emoji.addClickListener(e -> {
                        task.run();
                        contextMenu.close();
                        layout.remove(e.getSource());
                    });
                });
        contextMenu.add(layout);
        return soundButton;
    }

    public static Button openChatButton(final Consumer<String> messageAction) {
        final var chatButton = new Button(Emoji.CHAT.value);
        final var contextMenu = new ContextMenu(chatButton);
        contextMenu.setOpenOnClick(true);
        final var textField = chatInput(messageAction);
        textField.setAutofocus(true);
        contextMenu.addOpenedChangeListener(e -> {
            if (e.isOpened()) {
                textField.focus();
            }
        });
        contextMenu.add(textField);
        return chatButton;
    }

    public static TextField chatInput(Consumer<String> messageAction) {
        final var textField = new TextField();
        textField.setWidthFull();
        textField.setLabel("Отправь всем сообщение! (отправка на Enter)");
        textField.setMaxLength(200);
        textField.addKeyPressListener(Key.ENTER, event -> {
            if (StringUtils.isBlank(textField.getValue())) {
                return;
            }
            messageAction.accept(textField.getValue());
            textField.clear();
        });
        return textField;
    }

    public static Button reactionButton(String emoji, Consumer<Emoji> action) {
        final var reactionButton = new Button(emoji);

        final var popover = new Popover();
        popover.setTarget(reactionButton);
        popover.setOpenOnClick(true);
        popover.setCloseOnOutsideClick(true);
        popover.setHideDelay(100);
        popover.addThemeVariants(PopoverVariant.ARROW);

        final var layout = horizontalLayoutCenter();
        Emoji.REACTION_LIST.forEach(reaction -> {
            final var emojiSmall = emojiSmall(reaction.value);
            emojiSmall.addClickListener(e -> action.accept(reaction));
            layout.add(emojiSmall);
        });
        popover.add(layout);

        return reactionButton;
    }
}
