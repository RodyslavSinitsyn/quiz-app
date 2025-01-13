package org.rsinitsyn.quiz.component.cleverest;

import com.flowingcode.vaadin.addons.carousel.Carousel;
import com.flowingcode.vaadin.addons.carousel.Slide;
import com.vaadin.flow.component.*;
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
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.jfancy.StarsRating;
import lombok.experimental.UtilityClass;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@UtilityClass
public class CleverestComponents {

    public static final String LARGE_IMAGE_HEIGHT = "30em";
    public static final String MEDIUM_IMAGE_HEIGHT = "17.5em";
    public static final String SMALL_IMAGE_HEIGHT = "12.5em";

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

    public Span userAnswerSpan(UserGameState userGameState, QuestionType questionType, String... classes) {
        Span userAnswer = new Span();
        if (questionType.equals(QuestionType.PHOTO)) {
            userAnswer.add(QuizComponents.largeAvatar(userGameState.getLastAnswerText()));
        } else {
            userAnswer.add(String.valueOf(userGameState.getLastAnswerText()));
        }
        userAnswer.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
        userAnswer.addClassNames(classes);

        return new Span(
                userNameSpan(userGameState.getUsername(), userGameState.getColor(), classes),
                delimiterSpan(classes),
                userAnswer);
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
        span.getStyle().set("white-space", "pre-line");
        if (questionModel.getType().equals(QuestionType.PHOTO)) {
            span.add(new Image() {{
                setMaxHeight(MEDIUM_IMAGE_HEIGHT);
                setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getFirstCorrectAnswer().getPhotoFilename()));
            }});
        } else {
            span.setText(questionModel.getCorrectAnswersAsText());
        }
        return span;
    }

    public Span correctAnswerDescriptionSpan(QuestionModel questionModel, String... classes) {
        Span span = new Span();
        span.addClassNames(classes);
        span.addClassNames(LumoUtility.TextAlignment.CENTER,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY);
        span.setWidthFull();
        span.getStyle().set("white-space", "pre-line");
        span.setText(questionModel.getAnswerDescription());
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

    public Div optionComponent(String text,
                               int maxLength,
                               ComponentEventListener<ClickEvent<Div>> eventHandler) {
        var option = new Div();
        option.setWidthFull();
        option.setText(text);
        option.addClassNames(
                text.length() > maxLength
                        ? CleverestComponents.MOBILE_MEDIUM_FONT
                        : CleverestComponents.MOBILE_LARGE_FONT,
                LumoUtility.TextAlignment.CENTER,
                LumoUtility.TextColor.PRIMARY,
                LumoUtility.FontWeight.BOLD,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.BorderRadius.MEDIUM);
        option.addClickListener(eventHandler);
        return option;
    }

    public TextField answerInput(HasValue.ValueChangeListener<? super AbstractField.ComponentValueChangeEvent<TextField, String>> valueChangeHandler) {
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

    public Button primaryButton(String text, ComponentEventListener<ClickEvent<Button>> clickAction) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClickListener(clickAction);
        button.addClassNames(MOBILE_MEDIUM_FONT);
        return button;
    }

    public Button approveButton(Runnable clickAction,
                                int countLimit) {
        Button button = new Button();
        button.setIcon(VaadinIcon.CHECK.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
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
                    p.addClassNames(
                            LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY));
        });
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

    // TODO: Carousel for admin
    public static List<Component> userPhotoOptionsInputComponentsCarousel(QuestionModel questionModel) {
        List<AnswerModel> shuffledAnswers = questionModel.getShuffledAnswers();
        var slides = shuffledAnswers.stream()
                .map(answerModel -> {
                    Image image = new Image();
                    image.setSrc(QuizUtils.createStreamResourceForPhoto(answerModel.getPhotoFilename()));
                    image.setMaxHeight(LARGE_IMAGE_HEIGHT);
                    image.setWidthFull();
                    image.getStyle().set("object-fit", "contain");
                    image.getStyle().set("object-position", "center center");
                    return image;
                })
                .map(Slide::new)
                .toArray(Slide[]::new);

        Carousel carousel = new Carousel(slides).withAutoProgress();
        carousel.setWidthFull();
        carousel.setSlideDuration(3);
        carousel.setHeight(LARGE_IMAGE_HEIGHT);

//        Button prev = new Button("<<", event -> carousel.movePrev());
//        Button next = new Button(">>", event -> carousel.moveNext());
//        var nav = new HorizontalLayout(prev, next);
//        nav.setWidthFull();
//        nav.addClassNames(LumoUtility.Margin.Bottom.XLARGE);
//        nav.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
//        nav.setAlignItems(FlexComponent.Alignment.CENTER);

        return List.of(carousel);
    }
}
