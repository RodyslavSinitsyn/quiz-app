package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;

@Slf4j
public class QuestionForm extends FormLayout {
    FourAnswersQuestionBindingModel model = new FourAnswersQuestionBindingModel();
    TextArea text = new TextArea("Текст вопроса");
    ComboBox<String> category = new ComboBox<>();
    ComboBox<String> author = new ComboBox<>();
    TextField correctAnswerText = new TextField("Верный ответ");
    TextField secondOptionAnswerText = new TextField("Вариант 2");
    TextField thirdOptionAnswerText = new TextField("Вариант 3");
    TextField fourthOptionAnswerText = new TextField("Вариант 4");
    TextField photoLocation = new TextField("Ссылка на фото");

    Binder<FourAnswersQuestionBindingModel> binder = new BeanValidationBinder<>(FourAnswersQuestionBindingModel.class);

    private List<String> categoryList;
    private List<String> usersList;

    Button save = new Button("Сохранить");
    Button delete = new Button("Удалить");
    Button close = new Button("Отмена");

    public QuestionForm(List<QuestionCategoryEntity> categoryEntityList, List<UserEntity> usersList) {
        setUsersList(usersList);
        setCategoryList(categoryEntityList);
        configureInputs();
        add(text,
                correctAnswerText,
                secondOptionAnswerText,
                thirdOptionAnswerText,
                fourthOptionAnswerText,
                category,
                author,
                photoLocation,
                createButtonsLayout());
        binder.bindInstanceFields(this);
    }

    private void configureInputs() {
        text.setTooltipText("Shift + Enter для переноса");
        text.setMaxLength(FourAnswersQuestionBindingModel.TEXT_LENGTH_LIMIT);
        text.setValueChangeMode(ValueChangeMode.EAGER);
        text.addValueChangeListener(e -> {
            e.getSource().setHelperText(e.getValue().length() + "/" + text.getMaxLength());
        });
    }

    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, model)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        try {

            binder.writeBean(model);
            if (model.optionsRepeated()) {
                correctAnswerText.setErrorMessage("Варианты ответов должны быть уникальные");
                correctAnswerText.setInvalid(true);
                throw new IllegalArgumentException("Варианты ответов не валидны");
            }
            fireEvent(new SaveEvent(this, model));
        } catch (ValidationException | IllegalArgumentException e) {
            log.warn("Question form contains errors. {}", e.getMessage());
        }
    }


    public void setQuestion(FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
        this.model = fourAnswersQuestionBindingModel;
        binder.readBean(fourAnswersQuestionBindingModel);
    }

    public void setCategoryList(List<QuestionCategoryEntity> entities) {
        categoryList = entities.stream().map(QuestionCategoryEntity::getName).toList();
        category.setItems(categoryList);
        category.setLabel("Тема вопроса");
    }

    public void setUsersList(List<UserEntity> users) {
        usersList = users.stream().map(UserEntity::getUsername).toList();
        author.setItems(usersList);
        author.setLabel("Автор");
    }

    public static abstract class QuestionFormEvent extends ComponentEvent<QuestionForm> {
        private FourAnswersQuestionBindingModel model;

        protected QuestionFormEvent(QuestionForm source, FourAnswersQuestionBindingModel model) {
            super(source, false);
            this.model = model;
        }

        public FourAnswersQuestionBindingModel getQuestion() {
            return model;
        }
    }

    public static class SaveEvent extends QuestionFormEvent {
        SaveEvent(QuestionForm source, FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
            super(source, fourAnswersQuestionBindingModel);
        }
    }

    public static class DeleteEvent extends QuestionFormEvent {
        DeleteEvent(QuestionForm source, FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
            super(source, fourAnswersQuestionBindingModel);
        }

    }

    public static class CloseEvent extends QuestionFormEvent {
        CloseEvent(QuestionForm source) {
            super(source, null);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
