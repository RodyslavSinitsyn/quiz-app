package org.rsinitsyn.quiz.component.custom.form;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.model.binding.GuessPhotoQuestionBindingModel;

import java.util.List;
import java.util.function.Consumer;

import static org.rsinitsyn.quiz.utils.QuizComponents.*;

public class GuessPhotoQuestionForm extends AbstractQuestionCreationForm<GuessPhotoQuestionBindingModel> {

    private final Binder<GuessPhotoQuestionBindingModel> binder =
            new BeanValidationBinder<>(GuessPhotoQuestionBindingModel.class);

    private final HorizontalLayout photoContainer = new HorizontalLayout();
    private TextField answerText = new TextField("Ответ");

    public GuessPhotoQuestionForm(final List<QuestionCategoryEntity> categories) {
        super(categories);
        binder.bindInstanceFields(this);

        final var upload = uploadComponent("Фото подсказок",
                (memoryBuffer, event) ->
                        model.addPhotoStream(
                                event.getFileName(),
                                memoryBuffer.getInputStream(event.getFileName())),
                null, 10);
        upload.addFileRemovedListener(event -> {
            model.removePhotoStream(event.getFileName());
        });
        upload.addFileRejectedListener(event -> {
            infoNotification("Ошибка загрузки файла " + event.getErrorMessage());
        });

        add(text);
        add(answerText);
        add(upload);
        add(photoContainer);
        addCommonComponents();
    }

    private void renderPhotoContainer(GuessPhotoQuestionBindingModel model) {
        photoContainer.removeAll();
        final var button = new Button("Удалить все");
        button.addClickListener(event -> {
            model.cleanPhotos();
            photoContainer.removeAll();
        });
        photoContainer.add(button);
        model.getPhotoFilenames().forEach(filename -> {
            photoContainer.add(largeAvatar(filename));
        });
    }

    @Override
    protected Binder<GuessPhotoQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        binder.writeBean(model);
        if (model.getPhotoFilenameToResource().isEmpty()) {
            throw new IllegalStateException("Должна быть хотя бы одна фото подсказка");
        }
    }

    @Override
    protected Consumer<GuessPhotoQuestionBindingModel> afterModelSetAction() {
        return model -> {
            if (model != null) {
                renderPhotoContainer(model);
            }
        };
    }
}
