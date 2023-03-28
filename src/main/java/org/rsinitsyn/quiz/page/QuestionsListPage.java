package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.QuestionCategoryForm;
import org.rsinitsyn.quiz.component.QuestionForm;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Slf4j
@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
public class QuestionsListPage extends VerticalLayout {
    private H2 title = new H2("База вопросов");

    private Grid<QuestionEntity> grid;
    private TextField filterText = new TextField();
    private MultiSelectComboBox<QuestionCategoryEntity> categoryComboBox = new MultiSelectComboBox<>();
    private Dialog formDialog;
    private QuestionForm form;
    private QuestionCategoryForm categoryForm;

    private QuestionService questionService;
    private ImportService importService;
    private UserService userService;

    public QuestionsListPage(QuestionService questionService, ImportService importService, UserService userService) {
        this.questionService = questionService;
        this.importService = importService;
        this.userService = userService;

        setSizeFull();
        configureGrid();
        configureForm();
        configureCategoryForm();
        configureDialog();

        add(title, createToolbar(), grid);
    }

    // todo temp
    private void exportCode() {
        String res = questionService.findAll().stream().
                map(entity -> {
                    List<AnswerEntity> answers = entity.getAnswers().stream()
                            .sorted(Comparator.comparing(AnswerEntity::isCorrect, Comparator.reverseOrder()))
                            .toList();
                    StringJoiner joiner = new StringJoiner("|")
                            .add(entity.getText())
                            .add(answers.get(0).getText())
                            .add(answers.get(1).getText())
                            .add(answers.get(2).getText())
                            .add(answers.get(3).getText());
                    if (StringUtils.isNotEmpty(entity.getOriginalPhotoUrl())) {
                        joiner.add(entity.getOriginalPhotoUrl());
                    }
                    return joiner.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private void configureDialog() {
        formDialog = new Dialog();
        formDialog.close();
    }

    private void configureGrid() {
        grid = new Grid<>(QuestionEntity.class, false);
        grid.setSizeFull();
        grid.addColumn(new ComponentRenderer<>(entity -> {
                    HorizontalLayout row = new HorizontalLayout();
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    if (StringUtils.isNotEmpty(entity.getPhotoFilename())) {
                        Avatar smallPhoto = new Avatar();
                        smallPhoto.setImageResource(
                                QuizUtils.createStreamResourceForPhoto(entity.getPhotoFilename()));
                        row.add(smallPhoto);
                    }
                    row.add(new Span(
                            entity.getText().length() > 300
                                    ? entity.getText().substring(0, 300).concat("...")
                                    : entity.getText()));
                    return row;
                }))
                .setHeader("Текст")
                .setFlexGrow(5);
        grid.addColumn(QuestionEntity::getCreatedBy)
                .setHeader("Автор")
                .setSortable(true);
        grid.addColumn(entity -> entity.getCategory().getName())
                .setHeader("Тема")
                .setSortable(true);
        grid.addColumn(new LocalDateTimeRenderer<>(QuestionEntity::getCreationDate, QuizUtils.DATE_FORMAT_VALUE))
                .setHeader("Дата создания")
                .setSortable(true)
                .setComparator(Comparator.comparing(QuestionEntity::getCreationDate));
        grid.asSingleSelect().addValueChangeListener(event -> {
            editQuestion(ModelConverterUtils.toFourAnswersQuestionBindingModel(event.getValue()));
        });
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        updateList();
    }

    private void updateList() {
        grid.setItems(questionService.findAllByCurrentUser());
    }

    private void configureForm() {
        form = new QuestionForm(
                questionService.findAllCategories(),
                userService.findAllOrderByVisitDateDesc());
        form.setWidth("30em");
        form.addListener(QuestionForm.SaveEvent.class, event -> {
            questionService.saveOrUpdate(event.getQuestion());
            updateList();
            form.setQuestion(null);
            formDialog.close();
        });
        form.addListener(QuestionForm.DeleteEvent.class, event -> {
            questionService.deleteById(event.getQuestion().getId());
            updateList();
            form.setQuestion(null);
            formDialog.close();
        });
        form.addListener(QuestionForm.CloseEvent.class, event -> {
            form.setQuestion(null);
            formDialog.close();
        });
        form.setQuestion(null);
    }


    private void configureCategoryForm() {
        categoryForm = new QuestionCategoryForm(questionService.findAllCategories(), new QuestionCategoryBindingModel());
        categoryForm.setWidth("30em");
        categoryForm.addListener(QuestionCategoryForm.SaveCategoryEvent.class, event -> {
            questionService.saveQuestionCategory(event.getModel());
            formDialog.close();
            categoryForm.setModel(null);
            categoryForm.setCategories(questionService.findAllCategories());
            categoryComboBox.setItems(questionService.findAllCategories());
        });
        categoryForm.addListener(QuestionCategoryForm.CloseCategoryFormEvent.class, event -> {
            categoryForm.setModel(null);
            formDialog.close();
        });
    }

    private HorizontalLayout createToolbar() {
        filterText.setPlaceholder("Поиск...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(event ->
                updateListByFilter(entity -> entity.getText().contains(event.getValue())));
        filterText.setReadOnly(true); // TODO Back later

        categoryComboBox.setPlaceholder("Выбрать тему...");
        categoryComboBox.setItems(questionService.findAllCategories());
        categoryComboBox.setItemLabelGenerator(item -> item.getName());
        categoryComboBox.addSelectionListener(event -> {
            if (event.getValue().isEmpty()) {
                updateList();
            } else {
                updateListByFilter(entity -> event.getValue().contains(entity.getCategory()));
            }
        });

        Button addQuestionButton = new Button("Создать вопрос");
        addQuestionButton.addClickListener(event -> {
            grid.asSingleSelect().clear();
            editQuestion(new FourAnswersQuestionBindingModel());
        });

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setUploadButton(new Button("Импортировать вопросы"));
        upload.setDropLabel(new Label(""));
        upload.setAcceptedFileTypes(".txt");
        upload.addSucceededListener(event -> {
            InputStream inputStream = buffer.getInputStream();
            importService.importQuestions(inputStream);
            updateList();
        });

        Button addCategoryButton = new Button("Добавить тему");
        addCategoryButton.addClickListener(event -> {
            categoryForm.setModel(new QuestionCategoryBindingModel());
            addAndOpenDialog(categoryForm);
        });

        HorizontalLayout toolbar = new HorizontalLayout(categoryComboBox, addQuestionButton, upload, addCategoryButton);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        return toolbar;
    }

    private void editQuestion(FourAnswersQuestionBindingModel model) {
        if (model == null) {
            formDialog.close();
            form.setQuestion(null);
        } else {
            addAndOpenDialog(form);
            form.setCategoryList(questionService.findAllCategories());
            form.setQuestion(model);
        }
    }

    private void addAndOpenDialog(Component component) {
        formDialog.removeAll();
        formDialog.add(component);
        formDialog.open();
    }

    private void updateListByFilter(Predicate<? super QuestionEntity> filterCondition) {
        grid.setItems(questionService.findAllByCurrentUser().stream()
                .filter(filterCondition)
                .collect(Collectors.toList()));
    }
}
