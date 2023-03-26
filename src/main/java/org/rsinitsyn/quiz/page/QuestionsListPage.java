package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.InputStream;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.QuestionCategoryForm;
import org.rsinitsyn.quiz.component.QuestionForm;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.service.QuestionService;
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

    public QuestionsListPage(QuestionService questionService, ImportService importService) {
        this.questionService = questionService;
        this.importService = importService;

        setSizeFull();
        configureGrid();
        configureForm();
        configureCategoryForm();
        configureDialog();

        add(title, createToolbar(), grid);
    }

    private void configureDialog() {
        formDialog = new Dialog();
        formDialog.close();
    }

    private void configureGrid() {
        grid = new Grid<>(QuestionEntity.class, false);
        grid.setSizeFull();
        grid.addColumn(entity ->
                entity.getText().length() > 300
                        ? entity.getText().substring(0, 300).concat("...")
                        : entity.getText()
        ).setHeader("Текст").setFlexGrow(5);
        grid.addColumn(QuestionEntity::getCreatedBy).setHeader("Автор");
        grid.addColumn(entity -> entity.getCategory().getName()).setHeader("Тема");
        grid.addColumn(entity -> QuizUtils.formatDate(entity.getCreationDate())).setHeader("Дата создания");
        grid.asSingleSelect().addValueChangeListener(event -> {
            editQuestion(ModelConverterUtils.toFourAnswersQuestionBindingModel(event.getValue()));
        });
        grid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        updateList();
    }

    private void updateList() {
        grid.setItems(questionService.findAllByCurrentUser());
    }

    private void configureForm() {
        form = new QuestionForm(questionService.findAllCategories());
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
        filterText.setEnabled(false);

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
        upload.setAcceptedFileTypes(".txt");
        upload.addSucceededListener(event -> {
            InputStream inputStream = buffer.getInputStream();
            importService.importQuestions(inputStream);
            updateList();
        });

        Button addCategoryButton = new Button("Добавить категорию");
        addCategoryButton.addClickListener(event -> {
            categoryForm.setModel(new QuestionCategoryBindingModel());
            addAndOpenDialog(categoryForm);
        });

        HorizontalLayout toolbar = new HorizontalLayout(filterText, categoryComboBox, addQuestionButton, upload, addCategoryButton);
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
