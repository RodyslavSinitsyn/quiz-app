package org.rsinitsyn.quiz.component.quiz;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.AnswerHistory;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.model.quiz.QuizGameState;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Slf4j
public class QuizGameSettingsComponent extends FormLayout implements BeforeLeaveObserver {

    private static final String GENERAL_CATEGORY = "Общие";

    private QuizGameState gameState = new QuizGameState();
    private TextField gameName;
    private ComboBox<String> playerName = new ComboBox<>();
    private Checkbox answerOptionsEnabled = new Checkbox();
    private Checkbox timerEnabled = new Checkbox();
    private Checkbox hintsEnabled = new Checkbox();
    private Checkbox intrigueEnabled = new Checkbox();
    private MultiSelectListBox<QuestionModel> questions = new MultiSelectListBox<>();
    private Binder<QuizGameState> binder = new BeanValidationBinder<>(QuizGameState.class);

    private Checkbox selectAllCheckbox = new Checkbox();
    private Checkbox filterAnsweredCheckbox = new Checkbox();
    private Button playButton = new Button("Играть");

    private List<QuestionModel> questionModelList;
    private List<UserEntity> userEntityList;

    public QuizGameSettingsComponent(List<QuestionModel> questionModelList,
                                     List<UserEntity> userEntityList) {
        this.questionModelList = questionModelList;
        this.userEntityList = userEntityList;

        this.gameName = createTextInput("Название игры");
        configureUserComboBox();
        configureCheckBoxes();
        configureQuestionsList();
        configurePlayButton();

        binder.bindInstanceFields(this);

        setResponsiveSteps(new ResponsiveStep("0", 1));
        addClassNames(LumoUtility.AlignSelf.CENTER,
                LumoUtility.Padding.MEDIUM);
        getStyle().set("background-color", "white");
        setWidth("50em");

        add(QuizComponents.mainHeader("Настройки игры"),
                gameName,
                playerName,
                answerOptionsEnabled,
                timerEnabled,
                hintsEnabled,
                intrigueEnabled,
                new Hr(),
                selectAllCheckbox,
                filterAnsweredCheckbox,
                questions, playButton);
    }

    private TextField createTextInput(String labelText) {
        TextField field = new TextField(labelText);
        field.setValueChangeMode(ValueChangeMode.ON_BLUR);
        field.addValueChangeListener(event -> {
            binder.writeBeanAsDraft(gameState);
        });
        return field;
    }

    private void configureUserComboBox() {
        playerName.setLabel("Игрок");
        playerName.setItems(userEntityList.stream().map(UserEntity::getUsername).toList());
        playerName.addValueChangeListener(event -> {
            filterAnsweredCheckbox.setEnabled(StringUtils.isNotBlank(event.getValue()));
            configureQuestionsList();
        });
    }

    private void configureCheckBoxes() {
        answerOptionsEnabled.setLabel("Варианты ответов");
        answerOptionsEnabled.setTooltipText("Для некоторых вопросов будут отключены варианты ответов");
        answerOptionsEnabled.setValue(true);

        timerEnabled.setLabel("Таймер");
        timerEnabled.setValue(false);
        timerEnabled.setReadOnly(true);
        timerEnabled.setEnabled(false);

        hintsEnabled.setLabel("Включить подсказки");
        hintsEnabled.setTooltipText("[50 на 50], [3/4]");
        hintsEnabled.setValue(true);

        intrigueEnabled.setLabel("Включить интригу");
        intrigueEnabled.setTooltipText("Результат ответа будет отображен с задержкой, для интриги");
        intrigueEnabled.setValue(true);

        filterAnsweredCheckbox.setLabel("Скрыть пройденые вопросы");
        filterAnsweredCheckbox.setValue(false);
        filterAnsweredCheckbox.addClickListener(event -> {
            configureQuestionsList();
        });
        filterAnsweredCheckbox.setEnabled(StringUtils.isNoneBlank(playerName.getValue()));

        selectAllCheckbox.setLabel("Выбрать все");
        selectAllCheckbox.setValue(false);
        selectAllCheckbox.addClickListener(event -> {
            if (event.getSource().getValue()) {
                questions.select(questionModelList);
            } else {
                questions.deselectAll();
            }
        });
    }

    private void configureQuestionsList() {
        Label label = new Label();

        questions.removeAll();
        questions.setId("questions-list-box");
        questions.addClassNames(LumoUtility.Margin.MEDIUM);
        questions.setRenderer(createQuestionRowComponent());

        questions.addSelectionListener(event -> {
            selectAllCheckbox.setValue(
                    event.getValue().size() == questionModelList.size());
            playButton.setEnabled(!event.getValue().isEmpty());
            label.setText("Выбрано вопросов: " + event.getValue().size());
        });
        configureContentForQuestions();

        label.setFor(questions);
        label.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD);
        label.setText("Выбрано вопросов");
        questions.addComponentAsFirst(label);
    }

    private ComponentRenderer<HorizontalLayout, QuestionModel> createQuestionRowComponent() {
        return new ComponentRenderer<>(question -> {
            HorizontalLayout row = new HorizontalLayout();
            row.getStyle().set("border-bottom", "1px solid blue");
            row.getStyle().set("padding", ".25em");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            appendIconToQuestionRowComponent(row, question);
            if (StringUtils.isNotEmpty(question.getPhotoFilename())) {
                Avatar smallPhoto = new Avatar();
                smallPhoto.setImageResource(QuizUtils.createStreamResourceForPhoto(question.getPhotoFilename()));
                row.add(smallPhoto);
            }
            Span spanText = new Span(question.getText());
            spanText.addClassNames(LumoUtility.FontWeight.MEDIUM);
            row.add(spanText);

            row.add(QuizComponents.questionMechanicSpan(question.isOptionsOnly(), question.getType()));
            return row;
        });
    }

    private void appendIconToQuestionRowComponent(HorizontalLayout row, QuestionModel question) {
        if (playerName.getValue() != null) {
            AnswerHistory answerHistory = question.getPlayersAnswersHistory().get(playerName.getValue());
            if (answerHistory != null) {
                Icon icon;
                if (answerHistory.equals(AnswerHistory.ANSWERED_CORRECT)) {
                    icon = VaadinIcon.WARNING.create();
                    icon.setTooltipText("Игрок уже давал верный ответ на этот вопрос");
                    row.add(icon);
                } else if (answerHistory.equals(AnswerHistory.ANSWERED_WRONG)) {
                    icon = VaadinIcon.QUESTION.create();
                    icon.setTooltipText("Игрок овечал на вопрос, но неправильно");
                    row.add(icon);
                }
            }
        }
    }


    private void configureContentForQuestions() {
        if (questionModelList.isEmpty()) {
            questions.setEnabled(false);
            return;
        }
        List<QuestionModel> filteredListCopy = questionModelList.stream()
                .sorted(getCategoryComparator())
                .filter(questionModel -> {
                    if (!filterAnsweredCheckbox.getValue()) {
                        return Boolean.TRUE;
                    }
                    return Optional.ofNullable(questionModel.getPlayersAnswersHistory().get(playerName.getValue()))
                            .map(answerHistory -> !answerHistory.equals(AnswerHistory.ANSWERED_CORRECT))
                            .orElse(Boolean.TRUE);
                })
                .toList();
        questions.setItems(filteredListCopy);

        var category2QuestionsMap = filteredListCopy.stream()
                .collect(Collectors.groupingBy(QuestionModel::getCategoryName, LinkedHashMap::new, Collectors.toList()));

        List<QuestionModel> generalQuestionList = category2QuestionsMap.get(GENERAL_CATEGORY);
        if (generalQuestionList == null) {
            return;
        }
        AtomicReference<QuestionModel> lastModel =
                new AtomicReference<>(generalQuestionList.get(generalQuestionList.size() - 1));
        category2QuestionsMap.entrySet().stream().skip(1).forEach(entry -> {
            questions.addComponents(lastModel.get(), new Span(entry.getKey()), new Hr());
            lastModel.set(entry.getValue().get(entry.getValue().size() - 1));
        });
    }

    private void configurePlayButton() {
        playButton.setEnabled(false);
        playButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        playButton.addClickListener(event -> {
            try {
                binder.writeBean(gameState);
                fireEvent(new StartGameEvent(this, gameState));
            } catch (ValidationException e) {
                log.warn(e.getMessage());
            }
        });
    }

    private Comparator<QuestionModel> getCategoryComparator() {
        return (q1, q2) -> {
            if (q1.getCategoryName().equals(GENERAL_CATEGORY)) {
                return q2.getCategoryName().equals(GENERAL_CATEGORY) ? 0 : -1;
            }
            if (q2.getCategoryName().equals(GENERAL_CATEGORY)) {
                return 1;
            }
            return q1.getCategoryName().compareTo(q2.getCategoryName());
        };
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
    }

    @Getter
    public static class StartGameEvent extends ComponentEvent<QuizGameSettingsComponent> {
        private QuizGameState gameState;

        public StartGameEvent(QuizGameSettingsComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public StartGameEvent(QuizGameSettingsComponent source, QuizGameState gameState) {
            this(source, false);
            this.gameState = gameState;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
