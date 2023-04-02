package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.сustom.AnswerField;

@Slf4j
public class QuestionForm extends FormLayout {

    FourAnswersQuestionBindingModel questionModel = new FourAnswersQuestionBindingModel();
    TextArea text = new TextArea("Текст вопроса");
    ComboBox<String> category = new ComboBox<>();
    ComboBox<String> author = new ComboBox<>();
    VerticalLayout inputsLayout = new VerticalLayout();
    List<AnswerField> answers = new ArrayList<>();
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
        configureTextInput();

        inputsLayout.setAlignItems(FlexComponent.Alignment.START);
        inputsLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        inputsLayout.setMargin(false);
        inputsLayout.setPadding(false);
        inputsLayout.setSpacing(false);

        Upload uploadComponent = QuizComponents.uploadComponent("Импортировать аудио",
                (buffer, event) -> {
                    InputStream inputStream = buffer.getInputStream();
                    questionModel.setAudio(inputStream);
                }, ".mp3");

        add(text);
        add(inputsLayout);
        add(category,
                author,
                photoLocation,
                uploadComponent,
                createButtonsLayout());
        binder.bindInstanceFields(this);
    }

    private void configureAnswerInputs() {
        cleanupAnswers();
        if (questionModel.getAnswers().isEmpty()) {
            questionModel.initWith4Answers();
        }
        for (FourAnswersQuestionBindingModel.AnswerBindingModel answerBindingModel :
                questionModel.getAnswers()) { // TODO REFACTOR
            AnswerField answerField = new AnswerField(answerBindingModel);
            answerField.setWidthFull();
            answers.add(answerField);
            inputsLayout.add(answerField);
            binder.forField(answerField)
                    .withValidator(a -> a != null && StringUtils.isNotEmpty(a.getText()), "пустой вопрос")
                    .bind(m -> m.getAnswers().stream().filter(a -> a.getIndex() == answerField.getIndex()).findFirst().orElse(null),
                            (m, value) -> m.getAnswers().set(value.getIndex(), value));
        }
    }

    private void cleanupAnswers() {
        inputsLayout.removeAll();
        answers.forEach(binder::removeBinding);
        answers.clear();
    }

    private void configureTextInput() {
        text.setTooltipText("Shift + Enter для переноса");
        text.setSizeFull();
        text.setMaxLength(FourAnswersQuestionBindingModel.TEXT_LENGTH_LIMIT);
        text.setValueChangeMode(ValueChangeMode.EAGER);
        text.addValueChangeListener(e -> {
            e.getSource().setHelperText(e.getValue().length() + "/" + text.getMaxLength());
        });
        text.setRequired(true);
    }

    private HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        close.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndSave());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, questionModel)));
        close.addClickListener(event -> fireEvent(new CloseEvent(this)));

        return new HorizontalLayout(save, delete, close);
    }

    private void validateAndSave() {
        try {

            binder.writeBean(questionModel);
            if (questionModel.optionsRepeated()) {
                answers.get(0).setErrorMessage("Варианты ответов должны быть уникальные");
                answers.get(0).setInvalid(true);
                throw new IllegalArgumentException("Варианты ответов не валидны");
            }
            if (questionModel.noCorrectOption()) {
                answers.get(0).setErrorMessage("Не указан верный ответ");
                answers.get(0).setInvalid(true);
                throw new IllegalArgumentException("Не выбран верны ответ");
            }
            fireEvent(new SaveEvent(this, questionModel));
        } catch (ValidationException | IllegalArgumentException e) {
            log.warn("Question form contains errors. {}", e.getMessage());
        }
    }


    public void setQuestion(FourAnswersQuestionBindingModel model) {
        this.questionModel = model;
        if (model != null) {
            configureAnswerInputs();
        }
        binder.readBean(model);
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
