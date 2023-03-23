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
import com.vaadin.flow.shared.Registration;
import java.util.List;
import lombok.Getter;
import org.rsinitsyn.quiz.model.FourAnswersQuestionModel;
import org.rsinitsyn.quiz.model.GameSettingsModel;
import org.rsinitsyn.quiz.model.ViktorinaQuestion;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;

public class GameSettingsComponent extends FormLayout {

    GameSettingsModel model = new GameSettingsModel();
    TextField gameName = new TextField("Название игры");
    TextField playerName = new TextField("Имя игрока");
    MultiSelectListBox<ViktorinaQuestion> questions = new MultiSelectListBox<>();
    Binder<GameSettingsModel> binder = new BeanValidationBinder<>(GameSettingsModel.class);

    H2 title = new H2("Настройки игры");
    Button playButton = new Button("Играть");

    List<FourAnswersQuestionModel> fourAnswersQuestionModelList;

    public GameSettingsComponent(List<FourAnswersQuestionModel> fourAnswersQuestionModelList) {
        this.fourAnswersQuestionModelList = fourAnswersQuestionModelList;
        configureBinder();
        configureQuestionsList();
        configurePlayButton();

        add(title, gameName, playerName, questions, playButton);
        setResponsiveSteps(new ResponsiveStep("0", 1));
        setWidth("50em");
    }

    private void configureBinder() {
        binder.bindInstanceFields(this);
    }

    private void configureQuestionsList() {
        questions.setId("questions-list-box");
        questions.setItems(ModelConverterUtils.toViktorinaQuestions(fourAnswersQuestionModelList));
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
        private GameSettingsModel model;

        public StartGameEvent(GameSettingsComponent source, boolean fromClient) {
            super(source, fromClient);
        }

        public StartGameEvent(GameSettingsComponent source, GameSettingsModel model) {
            this(source, false);
            this.model = model;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
