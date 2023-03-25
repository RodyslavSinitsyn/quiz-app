package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
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
import java.util.stream.Collectors;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.QuestionCategoryForm;
import org.rsinitsyn.quiz.component.QuestionForm;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;

@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
public class QuestionsListPage extends VerticalLayout {
    private H2 title = new H2("База вопросов");

    private Grid<QuestionEntity> grid;
    private TextField filterText;
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
        grid = new Grid<>(QuestionEntity.class);
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(entity -> entity.getText().length() > 30
                ? entity.getText().substring(0, 30).concat("...")
                : entity.getText()).setHeader("Текст");
        grid.addColumn(QuestionEntity::getCreatedBy).setHeader("Автор");
        grid.addColumn(entity -> entity.getCategory().getName()).setHeader("Тема");
        grid.addColumn(QuestionEntity::getCreationDate).setHeader("Дата создания");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.asSingleSelect().addValueChangeListener(event -> {
            editQuestion(ModelConverterUtils.toFourAnswersQuestionBindingModel(event.getValue()));
        });
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
        });
        categoryForm.addListener(QuestionCategoryForm.CloseCategoryFormEvent.class, event -> {
            categoryForm.setModel(null);
            formDialog.close();
        });
    }

    private HorizontalLayout createToolbar() {
        filterText = new TextField();
        filterText.setPlaceholder("Поиск...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(event -> filterList());

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

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addQuestionButton, upload, addCategoryButton);
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

    private void filterList() {
        grid.setItems(questionService.findAllByCurrentUser().stream()
                .filter(question -> question.getText().contains(filterText.getValue()))
                .collect(Collectors.toList()));
    }
}
