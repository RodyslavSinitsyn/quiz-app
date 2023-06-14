package org.rsinitsyn.quiz.component.cleverest;

import com.flowingcode.vaadin.addons.carousel.Carousel;
import com.flowingcode.vaadin.addons.carousel.Slide;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.jfancy.StarsRating;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.rsinitsyn.quiz.component.сustom.LinkQuestionsComponent;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.QuestionModel.AnswerModel;
import org.rsinitsyn.quiz.model.cleverest.UserGameState;
import org.rsinitsyn.quiz.utils.AudioUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

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

    public Button optionButton(String text, Runnable action) {
        Button button = new Button();
        button.setWidthFull();
        button.setText(text);
        button.addThemeVariants(ButtonVariant.LUMO_LARGE);
        button.addClassNames(
                LumoUtility.BorderColor.PRIMARY,
                LumoUtility.Border.ALL,
                text.length() >= 20 ? MOBILE_MEDIUM_FONT : MOBILE_LARGE_FONT);
        button.addClickListener(event -> {
            action.run();
        });
        return button;
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

    public Button primaryButton(String text, ComponentEventListener<ClickEvent<Button>> clickAction) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        button.addClickListener(clickAction);
        button.addClassNames(MOBILE_MEDIUM_FONT);
        return button;
    }

    public Button approveButton(Runnable clickAction, boolean counterMode) {
        Button button = new Button();
        button.setIcon(VaadinIcon.CHECK.create());
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> {
            if (counterMode) {
                String currText = event.getSource().getElement().getText();
                String newText = currText.isBlank()
                        ? "1"
                        : String.valueOf(Integer.parseInt(button.getText()) + 1);
                button.setText(newText);
            } else {
                button.setEnabled(false);
            }
            event.getSource().getParent().ifPresent(p ->
                    p.addClassNames(
                            LumoUtility.Background.PRIMARY_10, LumoUtility.Border.ALL, LumoUtility.BorderColor.PRIMARY));
            clickAction.run();
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
    public VerticalLayout questionLayout(QuestionModel questionModel,
                                         List<String> textContentClasses,
                                         String imageHeight,
                                         boolean isAdmin) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);
        layout.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        layout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        if (StringUtils.isNotEmpty(questionModel.getPhotoFilename())) {
            Image image = new Image();
            image.setSrc(QuizUtils.createStreamResourceForPhoto(questionModel.getPhotoFilename()));
            image.setMaxHeight(imageHeight);
            if (!isAdmin) image.setWidthFull();
            image.getStyle().set("object-fit", "cover");
            image.getStyle().set("object-position", "center center");
            layout.add(image);
        }

        Span categorySpan = new Span(questionModel.getCategoryName());
        categorySpan.addClassNames(LumoUtility.FontWeight.LIGHT, MOBILE_SMALL_FONT);
        layout.add(categorySpan);

        Span textContent = CleverestComponents.questionTextSpan(
                questionModel.getText(),
                textContentClasses.toArray(new String[]{}));

        layout.add(questionModel.getType().equals(QuestionType.PRECISION)
                ? new Span(VaadinIcon.STAR.create(), textContent)
                : textContent);

        if (StringUtils.isNotEmpty(questionModel.getAudioFilename())) {
            Button playAudioButton = new Button("Слушать", VaadinIcon.PLAY_CIRCLE.create());
            playAudioButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST,
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SMALL);
            playAudioButton.setEnabled(isAdmin);
            playAudioButton.addClickListener(event -> {
                AudioUtils.playSoundAsync(questionModel.getAudioFilename());
            });
            layout.add(playAudioButton);
            // TODO Play audio on each device
//            AudioPlayer audioPlayer = new AudioPlayer(QuizUtils.createStreamResourceForAudio(questionModel.getAudioFilename()));
//            layout.add(audioPlayer);
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

    public List<Component> userTopListInputComponents(QuestionModel questionModel,
                                                      Consumer<List<String>> answersConsumer) {
        int topSize = questionModel.getAnswers().size();
        VerticalLayout topListLayout = new VerticalLayout();
        topListLayout.setSpacing(false);
        topListLayout.setPadding(false);
        Button submit = CleverestComponents.primaryButton("Ответить", e -> {
        });
        submit.setEnabled(false);
        submit.setWidthFull();
        Button addToListButton = new Button(VaadinIcon.PLUS_CIRCLE.create());
        addToListButton.addClickShortcut(Key.ENTER);
        TextField textField = CleverestComponents.answerInput(event -> {
            addToListButton.setEnabled(!event.getValue().isBlank());
        });
        addToListButton.addClickListener(event -> {
            Button topListItem = new Button(textField.getValue());
            topListItem.addClassNames(MOBILE_MEDIUM_FONT);
            topListItem.setWidthFull();
            topListItem.addClickListener(e -> {
                topListLayout.remove(e.getSource());
                textField.setEnabled(topListLayout.getChildren().count() != topSize);
                submit.setEnabled(topListLayout.getChildren().count() == topSize);
            });
            topListLayout.add(topListItem);
            textField.setValue("");
            textField.setEnabled(topListLayout.getChildren().count() != topSize);
            submit.setEnabled(topListLayout.getChildren().count() == topSize);
        });
        addToListButton.setEnabled(false);
        submit.addClickListener(event -> {
            answersConsumer.accept(topListLayout.getChildren().map(component -> component.getElement().getText()).toList());
        });

        HorizontalLayout userInput = new HorizontalLayout(textField, addToListButton);
        userInput.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        userInput.setAlignItems(FlexComponent.Alignment.END);
        userInput.setWidthFull();

        return List.of(
                userInput,
                topListLayout,
                submit
        );
    }

    public List<Component> userTextOptionsInputComponents(QuestionModel questionModel,
                                                          boolean clickEnabled,
                                                          Consumer<AnswerModel> answerConsumer) {
        return simpleOptionsComponents(questionModel, answerConsumer, clickEnabled,
                new ComponentRenderer<Component, AnswerModel>(
                        answerModel -> optionButton(answerModel.getText(), () -> {
                        })));
    }

    public static List<Component> userPhotoOptionsInputComponents(QuestionModel questionModel,
                                                                  Consumer<AnswerModel> answerConsumer) {
        return simpleOptionsComponents(questionModel, answerConsumer, true, new ComponentRenderer<Component, AnswerModel>(
                answerModel -> {
                    Image image = new Image();
                    image.setSrc(QuizUtils.createStreamResourceForPhoto(answerModel.getPhotoFilename()));
                    image.setMaxHeight(SMALL_IMAGE_HEIGHT);
                    image.setWidthFull();
                    image.getStyle().set("object-fit", "cover");
                    image.getStyle().set("object-position", "center center");
                    image.addClassNames(
                            LumoUtility.AlignSelf.CENTER,
                            LumoUtility.Border.ALL,
                            LumoUtility.BorderRadius.MEDIUM,
                            LumoUtility.BorderColor.PRIMARY
                    );
                    return image;
                }));
    }

    private List<Component> simpleOptionsComponents(QuestionModel questionModel,
                                                    Consumer<AnswerModel> answerConsumer,
                                                    boolean clickActionEnabled,
                                                    ComponentRenderer<? extends Component, AnswerModel> componentRenderer) {
        ListBox<AnswerModel> options = new ListBox<>();

        Button submit = primaryButton("Ответить", event -> answerConsumer.accept(options.getValue()));
        submit.setVisible(clickActionEnabled);
        submit.setWidthFull();
        submit.setEnabled(false);

        options.setWidthFull();
        options.setReadOnly(!clickActionEnabled);
        options.setItems(questionModel.getShuffledAnswers());
        options.setRenderer(componentRenderer);
        options.addValueChangeListener(event -> submit.setEnabled(true));

        return List.of(options, submit);
    }

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

    public static List<Component> userMatchAnswersComponents(QuestionModel questionModel,
                                                             Consumer<List<MutablePair<AnswerModel, AnswerModel>>> listOfAnswerLinksConsumer) {
        Button submit = primaryButton("Ответить", event -> {
        });
        submit.setWidthFull();
        submit.setEnabled(false);

        LinkQuestionsComponent component = new LinkQuestionsComponent(questionModel);
        component.addPairLinkedEventListener(event -> submit.setEnabled(event.isDone()));

        submit.addClickListener(event -> listOfAnswerLinksConsumer.accept(component.getPairs()));

        return List.of(component, submit);
    }
}
