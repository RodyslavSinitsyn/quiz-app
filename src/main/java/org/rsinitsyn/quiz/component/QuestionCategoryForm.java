package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;

@Slf4j
public class QuestionCategoryForm extends FormLayout {
    private QuestionCategoryBindingModel model;
    private TextField categoryName;
    private Binder<QuestionCategoryBindingModel> binder = new BeanValidationBinder<>(QuestionCategoryBindingModel.class);

    private Button saveButton;
    private Button cancelButton;

    private List<QuestionCategoryEntity> categories;

    public QuestionCategoryForm(List<QuestionCategoryEntity> categories, QuestionCategoryBindingModel model) {
        this.categories = categories;
        this.model = model;
        this.categoryName = createInput();
        this.cancelButton = createCancelButton();
        this.saveButton = createSaveButton();
        binder.forField(categoryName).withValidator(
                        value -> StringUtils.isNotBlank(value) && this.categories.stream()
                                .noneMatch(entity -> entity.getName().equalsIgnoreCase(value))
                        , "Категория уже существует")
                .bind(QuestionCategoryBindingModel::getCategoryName, QuestionCategoryBindingModel::setCategoryName);
        add(categoryName, saveButton, cancelButton);
    }

    private Button createSaveButton() {
        var button = new Button();
        button.addClickShortcut(Key.ENTER);
        button.setText("Создать");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(event -> {
            validateModel();
        });
        return button;
    }

    private Button createCancelButton() {
        var button = new Button();
        button.addClickShortcut(Key.ESCAPE);
        button.setText("Отменить");
        button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        button.addClickListener(event -> fireEvent(new CloseCategoryFormEvent(this)));
        return button;
    }

    private TextField createInput() {
        return new TextField("Имя категории");
    }

    private void validateModel() {
        try {
            binder.writeBean(model);
            fireEvent(new SaveCategoryEvent(this, model));
        } catch (ValidationException e) {
            log.warn(e.getMessage());
            throw new IllegalArgumentException("BLA");
        }
    }

    public void setModel(QuestionCategoryBindingModel model) {
        this.model = model;
        this.binder.readBean(model);
    }

    public void setCategories(List<QuestionCategoryEntity> categories) {
        this.categories = categories;
    }

    @Getter
    public static class SaveCategoryEvent extends ComponentEvent<QuestionCategoryForm> {
        private QuestionCategoryBindingModel model;

        SaveCategoryEvent(QuestionCategoryForm source) {
            super(source, false);
        }

        SaveCategoryEvent(QuestionCategoryForm source, QuestionCategoryBindingModel model) {
            this(source);
            this.model = model;
        }
    }

    @Getter
    public static class CloseCategoryFormEvent extends ComponentEvent<QuestionCategoryForm> {
        CloseCategoryFormEvent(QuestionCategoryForm source) {
            super(source, false);
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
