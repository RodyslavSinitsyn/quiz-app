package org.rsinitsyn.quiz.component.сustom.form;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.сustom.AnswerField;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.utils.QuizComponents;

@Slf4j
public class QuestionForm extends AbstractQuestionCreationForm<FourAnswersQuestionBindingModel> {

    private ComboBox<String> category = new ComboBox<>();
    private ComboBox<String> author = new ComboBox<>();
    private VerticalLayout inputsLayout = new VerticalLayout();
    private List<AnswerField> answers = new ArrayList<>();

    private final Binder<FourAnswersQuestionBindingModel> binder =
            new BeanValidationBinder<>(FourAnswersQuestionBindingModel.class);

    public QuestionForm(List<QuestionCategoryEntity> categoryEntityList, List<UserEntity> usersList) {
        binder.bindInstanceFields(this);

        setUsersList(usersList);
        setCategoryList(categoryEntityList);

        inputsLayout.setAlignItems(FlexComponent.Alignment.START);
        inputsLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);
        inputsLayout.setMargin(false);
        inputsLayout.setPadding(false);
        inputsLayout.setSpacing(false);

        add(text);
        add(inputsLayout);
        add(category,
                author,
                QuizComponents.uploadComponent("Импортировать аудио",
                        (buffer, event) -> {
                            InputStream inputStream = buffer.getInputStream();
                            model.setAudio(inputStream);
                        }, ".mp3"));

        addCommonComponents();
    }

    private void configureAnswerInputs() {
        cleanupAnswers();
        if (model.getAnswers().isEmpty()) {
            model.initWith4Answers();
        }
        for (FourAnswersQuestionBindingModel.AnswerBindingModel answerBindingModel :
                model.getAnswers()) { // TODO REFACTOR
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

    @Override
    public void setCategoryList(List<QuestionCategoryEntity> entities) {
        category.setItems(entities.stream().map(QuestionCategoryEntity::getName).toList());
        category.setLabel("Тема вопроса");
    }

    public void setUsersList(List<UserEntity> users) {
        author.setItems(users.stream().map(UserEntity::getUsername).toList());
        author.setLabel("Автор");
    }

    @Override
    protected Binder<FourAnswersQuestionBindingModel> getBinder() {
        return binder;
    }

    @Override
    protected void validate() throws ValidationException {
        binder.writeBean(model);
        if (model.optionsRepeated()) {
            answers.get(0).setErrorMessage("Варианты ответов должны быть уникальные");
            answers.get(0).setInvalid(true);
            throw new IllegalArgumentException("Варианты ответов не валидны");
        }
        if (model.noCorrectOption()) {
            answers.get(0).setErrorMessage("Не указан верный ответ");
            answers.get(0).setInvalid(true);
            throw new IllegalArgumentException("Не выбран верны ответ");
        }
    }

    @Override
    protected Consumer<FourAnswersQuestionBindingModel> afterModelSetAction() {
        return model -> {
            if (model != null) {
                configureAnswerInputs();
            }
        };
    }
}
