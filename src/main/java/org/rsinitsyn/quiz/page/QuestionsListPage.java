package org.rsinitsyn.quiz.page;

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
import org.rsinitsyn.quiz.component.QuestionForm;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.model.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;

@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
public class QuestionsListPage extends VerticalLayout {
    private Grid<FourAnswersQuestionBindingModel> grid;
    private TextField filterText = new TextField();
    private Dialog questionFormDialog;
    private QuestionForm form;
    private QuestionDao questionDao;
    private ImportService importService;
    private H2 title = new H2("База вопросов");

    public QuestionsListPage(QuestionDao questionDao, ImportService importService) {
        this.questionDao = questionDao;
        this.importService = importService;

        setSizeFull();
        configureGrid();
        configureForm();
        configureDialog();

        add(title, createToolbar(), grid);
    }

    private void configureDialog() {
        questionFormDialog = new Dialog(form);
        questionFormDialog.close();
        questionFormDialog.setHeaderTitle("Управление");
    }

    private void configureGrid() {
        grid = new Grid<>(FourAnswersQuestionBindingModel.class);
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(FourAnswersQuestionBindingModel::getText).setHeader("Text");
        grid.addColumn(FourAnswersQuestionBindingModel::getCorrectAnswerText).setHeader("Answer");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.asSingleSelect().addValueChangeListener(event -> {
            editQuestion(event.getValue());
        });
        updateList();
    }

    private void updateList() {
        grid.setItems(ModelConverterUtils.toQuestionModels(questionDao.findAll()));
    }

    private void editQuestion(FourAnswersQuestionBindingModel fourAnswersQuestionBindingModel) {
        if (fourAnswersQuestionBindingModel == null) {
            closeForm();
        } else {
            questionFormDialog.open();
            form.setQuestion(fourAnswersQuestionBindingModel);
        }
    }

    private void configureForm() {
        form = new QuestionForm();
        form.setWidth("25em");
        form.addListener(QuestionForm.SaveEvent.class, event -> {
            questionDao.save(ModelConverterUtils.toQuestionEntity(event.getQuestion()));
            updateList();
            closeForm();
        });
        form.addListener(QuestionForm.DeleteEvent.class, event -> {
            QuestionEntity questionEntity = ModelConverterUtils.toQuestionEntity(event.getQuestion());
            questionDao.delete(questionEntity);
            updateList();
            closeForm();
        });
        form.addListener(QuestionForm.CloseEvent.class, event -> closeForm());
        form.setQuestion(null);
    }

    private void closeForm() {
        form.setQuestion(null);
        questionFormDialog.close();
    }

    private HorizontalLayout createToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(event -> filterList());

        Button addQuestionButton = new Button("Add question");
        addQuestionButton.addClickListener(event -> addQuestion());

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setUploadButton(new Button("Import Questions"));
        upload.setAcceptedFileTypes(".txt");
        upload.addSucceededListener(event -> {
            InputStream inputStream = buffer.getInputStream();
            importService.importQuestions(inputStream);
            updateList();
        });

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addQuestionButton, upload);
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        return toolbar;
    }

    private void addQuestion() {
        grid.asSingleSelect().clear();
        editQuestion(new FourAnswersQuestionBindingModel());
    }

    private void filterList() {
        grid.setItems(ModelConverterUtils.toQuestionModels(questionDao.findAll()).stream()
                .filter(question -> question.getText().contains(filterText.getValue()))
                .collect(Collectors.toList()));
    }
}
