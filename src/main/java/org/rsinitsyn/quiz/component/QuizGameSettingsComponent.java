package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
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
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.model.QuizGameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;

@Slf4j
public class QuizGameSettingsComponent extends FormLayout implements BeforeLeaveObserver {

    private static final String GENERAL_CATEGORY = "Общие";

    private QuizGameStateModel model = new QuizGameStateModel();
    private TextField gameName;
    private TextField playerName;
    private Checkbox answerOptionsEnabled = new Checkbox();
    private Checkbox timerEnabled = new Checkbox();
    private MultiSelectListBox<QuizQuestionModel> questions = new MultiSelectListBox<>();
    private Binder<QuizGameStateModel> binder = new BeanValidationBinder<>(QuizGameStateModel.class);

    private H2 title = new H2("Настройки игры");
    private Checkbox selectAllCheckbox = new Checkbox();
    private Button playButton = new Button("Играть");

    private List<QuizQuestionModel> quizQuestionModelList;

    public QuizGameSettingsComponent(List<QuizQuestionModel> quizQuestionModelList) {
        setResponsiveSteps(new ResponsiveStep("0", 1));
        setWidth("50em");

        this.quizQuestionModelList = quizQuestionModelList;
        this.gameName = createTextInput("Название игры");
        this.playerName = createTextInput("Игрок");

        configureCheckBoxes();
        configureBinder();
        configureQuestionsList();
        configurePlayButton();

        add(title, gameName, playerName, answerOptionsEnabled, timerEnabled, selectAllCheckbox, questions, playButton);
    }


    private TextField createTextInput(String labelText) {
        TextField field = new TextField(labelText);
        field.setValueChangeMode(ValueChangeMode.ON_BLUR);
        field.addValueChangeListener(event -> {
            binder.writeBeanAsDraft(model);
            fireEvent(new UpdateGameEvent(this, model));
        });
        return field;
    }

    private void configureCheckBoxes() {
        answerOptionsEnabled.setLabel("Варианты ответов");
        answerOptionsEnabled.setValue(true);
        answerOptionsEnabled.setReadOnly(true);
        answerOptionsEnabled.setEnabled(false);

        timerEnabled.setLabel("Таймер");
        timerEnabled.setValue(false);
        timerEnabled.setReadOnly(true);
        timerEnabled.setEnabled(false);

        selectAllCheckbox.setLabel("Выбрать все");
        selectAllCheckbox.setValue(false);
        selectAllCheckbox.addClickListener(event -> {
            if (event.getSource().getValue()) {
                questions.select(quizQuestionModelList);
            } else {
                questions.deselectAll();
            }
        });
    }

    private void configureBinder() {
        binder.bindInstanceFields(this);
    }

    private void configureQuestionsList() {
        questions.setId("questions-list-box");
        questions.addClassNames(LumoUtility.Margin.SMALL);
        questions.setTooltipText("Bla bla tool");
        questions.setRenderer(new ComponentRenderer<>(question -> {
            HorizontalLayout row = new HorizontalLayout();
            row.getStyle().set("border", "1px solid blue");
            row.getStyle().set("border-radius", ".25em");
            row.getStyle().set("padding", ".5em");
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            if (StringUtils.isNotEmpty(question.getPhotoFilename())) {
                Avatar smallPhoto = new Avatar();
                smallPhoto.setImageResource(
                        new StreamResource(question.getPhotoFilename(), () -> question.openStream()));
                row.add(smallPhoto);
            }
            row.add(new Span(question.getText()));
            return row;
        }));
        questions.addSelectionListener(event -> {
            selectAllCheckbox.setValue(
                    event.getValue().size() == quizQuestionModelList.size());
        });
        configureItemsForCategoryListBox();

        Label label = new Label();
        label.setFor(questions);
        label.addClassNames(LumoUtility.FontSize.MEDIUM);
        label.setText("Выберите список вопросов");

        questions.addComponentAsFirst(label);
    }

    private void configureItemsForCategoryListBox() {
        if (quizQuestionModelList.isEmpty()) {
            questions.setEnabled(false);
            return;
        }
        Comparator<QuizQuestionModel> specificComparator = (q1, q2) -> {
            if (q1.getCategoryName().equals(GENERAL_CATEGORY)) {
                return q2.getCategoryName().equals(GENERAL_CATEGORY) ? 0 : -1;
            }
            if (q2.getCategoryName().equals(GENERAL_CATEGORY)) {
                return 1;
            }
            return q1.getCategoryName().compareTo(q2.getCategoryName());
        };
        quizQuestionModelList = quizQuestionModelList.stream()
                .sorted(specificComparator)
                .toList();
        questions.setItems(quizQuestionModelList);

        var category2QuestionsMap = quizQuestionModelList.stream()
                .collect(Collectors.groupingBy(QuizQuestionModel::getCategoryName, LinkedHashMap::new, Collectors.toList()));

        List<QuizQuestionModel> generalQuestionList = category2QuestionsMap.get(GENERAL_CATEGORY);
        if (generalQuestionList == null) {
            return;
        }
        AtomicReference<QuizQuestionModel> lastModel =
                new AtomicReference<>(generalQuestionList.get(generalQuestionList.size() - 1));
        category2QuestionsMap.entrySet().stream().skip(1).forEach(entry -> {
            questions.addComponents(lastModel.get(), new Span(entry.getKey()), new Hr());
            lastModel.set(entry.getValue().get(entry.getValue().size() - 1));
        });
    }

    private void configurePlayButton() {
        playButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        playButton.addClickListener(event -> {
            try {
                binder.writeBean(model);
                fireEvent(new StartGameEvent(this, model));
            } catch (ValidationException e) {
                log.warn(e.getMessage());
            }
        });
        playButton.setEnabled(!quizQuestionModelList.isEmpty());
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        model.getQuestions().forEach(QuizQuestionModel::closePhotoStream);
    }

    @Getter
    public static class StartGameEvent extends ComponentEvent<QuizGameSettingsComponent> {
        private QuizGameStateModel model;

        public StartGameEvent(QuizGameSettingsComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public StartGameEvent(QuizGameSettingsComponent source, QuizGameStateModel model) {
            this(source, false);
            this.model = model;
        }
    }

    @Getter
    public static class UpdateGameEvent extends ComponentEvent<QuizGameSettingsComponent> {
        private QuizGameStateModel model;

        public UpdateGameEvent(QuizGameSettingsComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public UpdateGameEvent(QuizGameSettingsComponent source, QuizGameStateModel model) {
            this(source, false);
            this.model = model;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
