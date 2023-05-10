package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.InputStream;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.сustom.PrecisionQuestionForm;
import org.rsinitsyn.quiz.component.сustom.QuestionCategoryForm;
import org.rsinitsyn.quiz.component.сustom.QuestionForm;
import org.rsinitsyn.quiz.component.сustom.QuestionListGrid;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;

@Slf4j
@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
public class QuestionsListPage extends VerticalLayout {

    private QuestionListGrid grid;
    private TextField filterText = new TextField();
    private MultiSelectComboBox<QuestionCategoryEntity> categoryFilter = new MultiSelectComboBox<>();
    private HorizontalLayout groupedOperations = new HorizontalLayout();
    private Dialog formDialog;
    private QuestionForm form;
    private QuestionCategoryForm categoryForm;
    private PrecisionQuestionForm precisionQuestionForm;

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
        configurePrecisionForm();
        configureDialog();

        add(QuizComponents.mainHeader("База вопросов"), createToolbar(), grid);
    }

    private void configureDialog() {
        formDialog = new Dialog();
        formDialog.close();
    }

    private void configureGrid() {
        grid = new QuestionListGrid(Collections.emptyList());
        grid.addColumn(new ComponentRenderer<>(QuizComponents::questionLinkedWithGameIcon))
                .setHeader("Связь")
                .setFlexGrow(0);
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addSelectionListener(event -> {
            if (!event.isFromClient()) {
                return;
            }
            groupedOperations.setVisible(!event.getAllSelectedItems().isEmpty());
        });
        grid.addItemClickListener(event -> {
            grid.select(event.getItem());
            if (event.getItem().getType().equals(QuestionType.PRECISION)) {
                precisionQuestionForm.setModel(ModelConverterUtils.toPrecisionQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(precisionQuestionForm);
            } else {
                editQuestion(ModelConverterUtils.toFourAnswersQuestionBindingModel(event.getItem()));
            }
        });
        grid.addDefaultColumns();
        updateList();
    }

    private void updateList() {
        grid.setItems(questionService.findAllCreatedByCurrentUser());
    }

    private void configureForm() {
        form = new QuestionForm(
                questionService.findAllCategories(),
                userService.findAllOrderByVisitDateDesc());
        form.setWidth("30em");
        form.setHeightFull();
        form.addListener(QuestionForm.SaveEvent.class, event -> {
            questionService.saveOrUpdate(event.getQuestion());
            updateList();
            form.setQuestion(null);
            formDialog.close();
            grid.asMultiSelect().clear();
        });
        form.addListener(QuestionForm.DeleteEvent.class, event -> {
            questionService.deleteById(event.getQuestion().getId());
            updateList();
            form.setQuestion(null);
            formDialog.close();
            grid.asMultiSelect().clear();
        });
        form.addListener(QuestionForm.CancelEvent.class, event -> {
            form.setQuestion(null);
            formDialog.close();
        });
        form.setQuestion(null);
    }

    private void configurePrecisionForm() {
        precisionQuestionForm = new PrecisionQuestionForm();
        precisionQuestionForm.addListener(PrecisionQuestionForm.SaveEvent.class, event -> {
            questionService.saveOrUpdate(event.getSource().getModel());
            updateList();
            precisionQuestionForm.setModel(null);
            formDialog.close();
        });
        precisionQuestionForm.addListener(PrecisionQuestionForm.CancelEvent.class, event -> {
            precisionQuestionForm.setModel(null);
            formDialog.close();
        });
        precisionQuestionForm.setModel(null);
    }

    private void configureCategoryForm() {
        categoryForm = new QuestionCategoryForm(questionService.findAllCategories(), new QuestionCategoryBindingModel());
        categoryForm.setWidth("30em");
        categoryForm.addListener(QuestionCategoryForm.SaveCategoryEvent.class, event -> {
            questionService.saveQuestionCategory(event.getModel());
            formDialog.close();
            categoryForm.setModel(null);
            categoryForm.setCategories(questionService.findAllCategories());
            categoryFilter.setItems(questionService.findAllCategories());
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

        categoryFilter.setPlaceholder("Выбрать тему...");
        categoryFilter.setItems(questionService.findAllCategories());
        categoryFilter.setItemLabelGenerator(item -> item.getName());
        categoryFilter.addSelectionListener(event -> {
            if (event.getValue().isEmpty()) {
                updateList();
            } else {
                updateListByFilter(entity -> event.getValue().contains(entity.getCategory()));
            }
        });

        Button addQuestionButton = new Button("Создать вопрос");
        addQuestionButton.addClickListener(event -> {
            grid.asMultiSelect().clear();
            editQuestion(new FourAnswersQuestionBindingModel());
        });

        Button addPrecisionQuestionButton = new Button("Создать точный вопрос");
        addPrecisionQuestionButton.addClickListener(event -> {
            grid.asMultiSelect().clear();
            precisionQuestionForm.setModel(new PrecisionQuestionBindingModel());
            addToDialogAndOpen(precisionQuestionForm);
        });

        Upload uploadComponent = QuizComponents.uploadComponent(
                "Импортировать",
                (buffer, event) -> {
                    InputStream inputStream = buffer.getInputStream();
                    importService.importQuestions(inputStream);
                    updateList();
                }, ".txt");

        Button addCategoryButton = new Button("Добавить тему");
        addCategoryButton.addClickListener(event -> {
            categoryForm.setModel(new QuestionCategoryBindingModel());
            addToDialogAndOpen(categoryForm);
        });

        configureGroupedActions();

        HorizontalLayout toolbar = new HorizontalLayout(
                categoryFilter,
                addQuestionButton,
                addPrecisionQuestionButton,
                uploadComponent,
                addCategoryButton,
                groupedOperations);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setJustifyContentMode(JustifyContentMode.CENTER);
        return toolbar;
    }

    private void configureGroupedActions() {
        groupedOperations.setVisible(false);

        Button deleteAllButton = new Button("Удалить");
        deleteAllButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteAllButton.setIcon(VaadinIcon.CLOSE_SMALL.create());
        deleteAllButton.addClickListener(event -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setCancelable(true);
            dialog.setCloseOnEsc(true);
            dialog.setHeader("Удалить все вопросы ниже?");
            Span text = new Span(grid.getSelectedItems().stream()
                    .map(QuestionEntity::getText)
                    .collect(Collectors.joining(System.lineSeparator())));
            text.getStyle().set("white-space", "pre-line");
            dialog.setText(text);
            dialog.addConfirmListener(e -> {
                questionService.deleteAll(grid.getSelectedItems());
                dialog.close();
                grid.asMultiSelect().deselectAll();
                updateList();
                groupedOperations.setVisible(false);
            });
            dialog.open();
        });

        Button updateCategoryButton = new Button("Обновить категорию");
        updateCategoryButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        updateCategoryButton.setIcon(VaadinIcon.EDIT.create());
        updateCategoryButton.addClickListener(event -> {
            ConfirmDialog dialog = new ConfirmDialog();
            dialog.setCancelable(true);
            dialog.setCloseOnEsc(true);
            dialog.setHeader("Выберите тему");

            Select<QuestionCategoryEntity> select = new Select<>();
            select.setPlaceholder("Тема");
            select.setItems(questionService.findAllCategories());
            select.setRenderer(new ComponentRenderer<Component, QuestionCategoryEntity>(
                    category -> new Span(category.getName())));

            dialog.add(select);
            dialog.addConfirmListener(e -> {
                questionService.updateCategory(grid.getSelectedItems(), select.getValue());
                dialog.close();
                grid.asMultiSelect().deselectAll();
                updateList();
                groupedOperations.setVisible(false);
            });
            dialog.open();
        });

        Button updateOptionsOnly = new Button("Поменять механику");
        updateOptionsOnly.setTooltipText("Вопросы которые можно задать только с вариантами становятся сингл и наоборот." +
                "По усмотрению автора");
        updateOptionsOnly.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        updateOptionsOnly.setIcon(VaadinIcon.OPTIONS.create());
        updateOptionsOnly.addClickListener(event -> {
            questionService.updateOptionsOnlyProperty(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
            updateList();
            groupedOperations.setVisible(false);
        });

        groupedOperations.add(deleteAllButton, updateCategoryButton, updateOptionsOnly);
    }

    private void editQuestion(FourAnswersQuestionBindingModel model) {
        if (model == null) {
            formDialog.close();
            form.setQuestion(null);
        } else {
            addToDialogAndOpen(form);
            form.setCategoryList(questionService.findAllCategories());
            form.setQuestion(model);
        }
    }

    private void addToDialogAndOpen(Component component) {
        formDialog.removeAll();
        formDialog.add(component);
        formDialog.open();
    }

    private void updateListByFilter(Predicate<? super QuestionEntity> filterCondition) {
        grid.setItems(questionService.findAllCreatedByCurrentUser().stream()
                .filter(filterCondition)
                .collect(Collectors.toList()));
    }
}
