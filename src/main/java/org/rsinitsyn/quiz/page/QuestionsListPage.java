package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
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
import org.rsinitsyn.quiz.model.FourAnswersQuestionModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;

@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
public class QuestionsListPage extends VerticalLayout {
    private Grid<FourAnswersQuestionModel> grid = new Grid<>(FourAnswersQuestionModel.class);
    private TextField filterText = new TextField();
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

        add(title, createToolbar(), createBody());
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addColumn(FourAnswersQuestionModel::getText).setHeader("Text");
        grid.addColumn(FourAnswersQuestionModel::getCorrectAnswerText).setHeader("Answer");
        grid.getColumns().forEach(col -> col.setAutoWidth(true));
        grid.asSingleSelect().addValueChangeListener(event -> {
            editQuestion(event.getValue());
        });
        updateList();
    }

    private void updateList() {
        grid.setItems(ModelConverterUtils.toQuestionModels(questionDao.findAll()));
    }

    private void editQuestion(FourAnswersQuestionModel fourAnswersQuestionModel) {
        if (fourAnswersQuestionModel == null) {
            closeForm();
        } else {
            form.setQuestion(fourAnswersQuestionModel);
            form.setVisible(true);
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
        closeForm();
    }

    private void closeForm() {
        form.setQuestion(null);
        form.setVisible(false);
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
        editQuestion(new FourAnswersQuestionModel());
    }

    private Component createBody() {
        HorizontalLayout body = new HorizontalLayout(grid, form);
        body.setFlexGrow(2, grid);
        body.setFlexGrow(1, form);
        body.setSizeFull();
        return body;
    }

    private void filterList() {
        grid.setItems(ModelConverterUtils.toQuestionModels(questionDao.findAll()).stream()
                .filter(question -> question.getText().contains(filterText.getValue()))
                .collect(Collectors.toList()));
    }
}
