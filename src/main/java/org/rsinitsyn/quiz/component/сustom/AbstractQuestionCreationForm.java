package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.shared.Registration;
import java.util.List;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;

@Slf4j
public abstract class AbstractQuestionCreationForm<T> extends FormLayout {

    @Getter
    protected T model;

    private final Button save = new Button("Сохранить");
    private final Button delete = new Button("Удалить");
    private final Button cancel = new Button("Отмена");

    public AbstractQuestionCreationForm() {
        setWidth("30em");
    }

    protected abstract Binder<T> getBinder();

    protected abstract void validate() throws ValidationException;

    protected abstract Consumer<T> afterModelSetAction();

    public void setModel(T model) {
        this.model = model;
        Consumer<T> afterAction = afterModelSetAction();
        if (afterAction != null) {
            afterAction.accept(this.model);
        }
        getBinder().readBean(model);
    }

    protected HorizontalLayout createButtonsLayout() {
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        save.addClickShortcut(Key.ENTER);
        cancel.addClickShortcut(Key.ESCAPE);

        save.addClickListener(event -> validateAndFireEvent());
        delete.addClickListener(event -> fireEvent(new DeleteEvent(this, model)));
        cancel.addClickListener(event -> fireEvent(new CancelEvent(this)));

        HorizontalLayout layout = new HorizontalLayout(save, cancel, delete);

        layout.setWidthFull();
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        layout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        layout.setAlignItems(FlexComponent.Alignment.START);

        return layout;
    }

    private void validateAndFireEvent() {
        try {
            validate();
            fireEvent(new SaveEvent(this, this.model));
        } catch (ValidationException e) {
            log.warn("Validation error: {}", e.getMessage());
        }
    }

    public void setCategoryList(List<QuestionCategoryEntity> allCategories) {
        // override if needed
    }

    public static abstract class QuestionFormEvent extends ComponentEvent<FormLayout> {
        @Getter
        private Object model;

        public QuestionFormEvent(AbstractQuestionCreationForm source) {
            super(source, false);
        }

        public QuestionFormEvent(AbstractQuestionCreationForm source, Object model) {
            this(source);
            this.model = model;
        }
    }

    public static class SaveEvent extends QuestionFormEvent {
        public SaveEvent(AbstractQuestionCreationForm source, Object model) {
            super(source, model);
        }
    }

    public static class DeleteEvent extends QuestionFormEvent {
        public DeleteEvent(AbstractQuestionCreationForm source, Object model) {
            super(source, model);
        }
    }

    @Getter
    public static class CancelEvent extends QuestionFormEvent {
        public CancelEvent(AbstractQuestionCreationForm source) {
            super(source);
        }
    }

    public Registration addSaveEventListener(Consumer<SaveEvent> eventConsumer) {
        return getEventBus().addListener(SaveEvent.class, eventConsumer::accept);
    }

    public Registration addDeleteEventListener(Consumer<DeleteEvent> eventConsumer) {
        return getEventBus().addListener(DeleteEvent.class, eventConsumer::accept);
    }

    public Registration addCancelEventListener(Consumer<CancelEvent> eventConsumer) {
        return getEventBus().addListener(CancelEvent.class, event -> {
            setModel(null);
            eventConsumer.accept(event);
        });
    }
}
