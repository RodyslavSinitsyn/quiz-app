package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.shared.Registration;
import java.util.List;
import lombok.Getter;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.GameStateModel;
import org.rsinitsyn.quiz.model.QuizQuestionModel;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;

public class GameSettingsComponent extends FormLayout {

    GameStateModel model = new GameStateModel();
    TextField gameName;
    TextField playerName;
    MultiSelectListBox<QuizQuestionModel> questions = new MultiSelectListBox<>();
    Binder<GameStateModel> binder = new BeanValidationBinder<>(GameStateModel.class);

    H2 title = new H2("Настройки игры");
    Button playButton = new Button("Играть");

    List<FourAnswersQuestionBindingModel> fourAnswersQuestionBindingModelList;

    public GameSettingsComponent(List<FourAnswersQuestionBindingModel> fourAnswersQuestionBindingModelList) {
        setResponsiveSteps(new ResponsiveStep("0", 1));
        setWidth("50em");

        this.fourAnswersQuestionBindingModelList = fourAnswersQuestionBindingModelList;
        this.gameName = createTextInput("Название игры");
        this.playerName = createTextInput("Создатель игры");
        configureBinder();
        configureQuestionsList();
        configurePlayButton();

        add(title, gameName, playerName, questions, playButton);
    }

    private void configureBinder() {
        binder.bindInstanceFields(this);
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

    private void configureQuestionsList() {
        questions.setId("questions-list-box");
        questions.setItems(ModelConverterUtils.toViktorinaQuestions(fourAnswersQuestionBindingModelList));
        questions.setRenderer(new ComponentRenderer<>(question -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.add(new Span(question.getText()));
            row.getStyle().set("border", "1px solid blue");
            return row;
        }));

        Label label = new Label();
        label.setFor(questions);
        label.setText("Выберите список вопросов");

        questions.addComponentAsFirst(label);
    }

    private void configurePlayButton() {
        playButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        playButton.addClickListener(event -> {
            try {
                binder.writeBean(model);
                fireEvent(new StartGameEvent(this, model));
            } catch (ValidationException e) {
                e.printStackTrace();
            }
        });
    }

    @Getter
    public static class StartGameEvent extends ComponentEvent<GameSettingsComponent> {
        private GameStateModel model;

        public StartGameEvent(GameSettingsComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public StartGameEvent(GameSettingsComponent source, GameStateModel model) {
            this(source, false);
            this.model = model;
        }
    }

    @Getter
    public static class UpdateGameEvent extends ComponentEvent<GameSettingsComponent> {
        private GameStateModel model;

        public UpdateGameEvent(GameSettingsComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public UpdateGameEvent(GameSettingsComponent source, GameStateModel model) {
            this(source, false);
            this.model = model;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
