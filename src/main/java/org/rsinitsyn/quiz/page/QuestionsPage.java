package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
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
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.сustom.AbstractQuestionCreationForm;
import org.rsinitsyn.quiz.component.сustom.OrQuestionForm;
import org.rsinitsyn.quiz.component.сustom.PrecisionQuestionForm;
import org.rsinitsyn.quiz.component.сustom.QuestionCategoryForm;
import org.rsinitsyn.quiz.component.сustom.QuestionForm;
import org.rsinitsyn.quiz.component.сustom.QuestionListGrid;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.FourAnswersQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.OrQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.PrecisionQuestionBindingModel;
import org.rsinitsyn.quiz.model.binding.QuestionCategoryBindingModel;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.SessionWrapper;

@Slf4j
@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
public class QuestionsPage extends VerticalLayout implements AfterNavigationObserver {

    private QuestionListGrid grid;
    private TextField filterText = new TextField();
    private MultiSelectComboBox<QuestionCategoryEntity> categoryFilter = new MultiSelectComboBox<>();
    private HorizontalLayout groupedOperations = new HorizontalLayout();
    private Span spinner = new Span();

    private Dialog formDialog;
    private QuestionCategoryForm categoryForm;
    private AbstractQuestionCreationForm<FourAnswersQuestionBindingModel> form;
    private AbstractQuestionCreationForm<PrecisionQuestionBindingModel> precisionForm;
    private AbstractQuestionCreationForm<OrQuestionBindingModel> orForm;

    private final QuestionService questionService;
    private final ImportService importService;
    private final UserService userService;

    public QuestionsPage(QuestionService questionService, ImportService importService, UserService userService) {
        this.questionService = questionService;
        this.importService = importService;
        this.userService = userService;

        configureGrid();
        configureForms();
        configureCategoryForm();
        configureDialog();

        add(QuizComponents.mainHeader("База вопросов"), createToolbar(), spinner, grid);
    }

    private void configureDialog() {
        formDialog = new Dialog();
        formDialog.close();
    }

    private void configureGrid() {
        grid = new QuestionListGrid(Collections.emptyList(), true);
        grid.addColumn(new ComponentRenderer<>(QuizComponents::questionLinkedWithGameIcon))
                .setHeader("Связь")
                .setFlexGrow(0);
        grid.addDefaultColumns();
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
                precisionForm.setModel(ModelConverterUtils.toPrecisionQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(precisionForm);
            } else if (event.getItem().getType().equals(QuestionType.OR)) {
                orForm.setModel(ModelConverterUtils.toOrQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(orForm);
            } else {
                editQuestion(ModelConverterUtils.toFourAnswersQuestionBindingModel(event.getItem()));
            }
        });
    }

    @SuppressWarnings("all")
    private void updateListAsync() {
        UI ui = getUI().orElse(null);
        String loggedUser = SessionWrapper.getLoggedUser();
        CompletableFuture.supplyAsync(() -> questionService.findAllCreatedByUser(loggedUser))
                .thenAccept(questionEntities -> ui.access(() -> {
                    grid.setQuestions(questionEntities);
                }));
    }

    private void configureForms() {
        // question form
        form = new QuestionForm(
                questionService.findAllCategories(),
                userService.findAllOrderByVisitDateDesc());
        form.setWidth("30em");
        form.setHeightFull();
        form.addSaveEventListener(event -> {
            questionService.saveOrUpdate((FourAnswersQuestionBindingModel) event.getModel());
            updateListAsync();
            formDialog.close();
            grid.asMultiSelect().clear();
        });
        form.addDeleteEventListener(event -> {
            questionService.deleteById(((FourAnswersQuestionBindingModel) event.getModel()).getId());
            updateListAsync();
            formDialog.close();
            grid.asMultiSelect().clear();
        });
        form.addCancelEventListener(event -> formDialog.close());
        form.setModel(null);

        // precision
        precisionForm = new PrecisionQuestionForm();
        precisionForm.addSaveEventListener(event -> {
            questionService.saveOrUpdate((PrecisionQuestionBindingModel) event.getModel());
            updateListAsync();
            formDialog.close();
        });
        precisionForm.addDeleteEventListener(event -> {
            questionService.deleteById(((PrecisionQuestionBindingModel) event.getModel()).getId());
            updateListAsync();
            formDialog.close();
        });
        precisionForm.addCancelEventListener(event -> formDialog.close());
        precisionForm.setModel(null);

        // or
        orForm = new OrQuestionForm();
        orForm.addSaveEventListener(event -> {
            questionService.saveOrUpdate((OrQuestionBindingModel) event.getModel());
            updateListAsync();
            formDialog.close();
        });
        orForm.addDeleteEventListener(event -> {
            questionService.deleteById(((OrQuestionBindingModel) event.getModel()).getId());
            updateListAsync();
            formDialog.close();
        });
        orForm.addCancelEventListener(event -> formDialog.close());
        orForm.setModel(null);
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
                updateListAsync();
            } else {
                updateListByFilter(entity -> event.getValue().contains(entity.getCategory()));
            }
        });

        Button addQuestionButton = new Button("Вопрос");
        addQuestionButton.addClickListener(event -> {
            grid.asMultiSelect().clear();
            editQuestion(new FourAnswersQuestionBindingModel());
        });

        Button addPrecisionQuestionButton = new Button("Точный вопрос");
        addPrecisionQuestionButton.addClickListener(event -> {
            grid.asMultiSelect().clear();
            precisionForm.setModel(new PrecisionQuestionBindingModel());
            addToDialogAndOpen(precisionForm);
        });

        Button addOrQuestionButton = new Button("Или вопрос");
        addOrQuestionButton.addClickListener(event -> {
            grid.asMultiSelect().clear();
            orForm.setModel(new OrQuestionBindingModel());
            addToDialogAndOpen(orForm);
        });

        Upload uploadComponent = QuizComponents.uploadComponent(
                "Импортировать",
                (buffer, event) -> {
                    InputStream inputStream = buffer.getInputStream();
                    importService.importQuestions(inputStream);
                    updateListAsync();
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
                addOrQuestionButton,
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
            Span text = new Span(grid.getSelectedItems().stream()
                    .map(QuestionEntity::getText)
                    .collect(Collectors.joining(System.lineSeparator())));
            text.getStyle().set("white-space", "pre-line");

            QuizComponents.openConfirmDialog(
                    text,
                    "Удалить все вопросы ниже?",
                    () -> {
                        questionService.deleteAll(grid.getSelectedItems());
                        grid.asMultiSelect().deselectAll();
                        updateListAsync();
                        groupedOperations.setVisible(false);
                    });
        });

        Button updateCategoryButton = new Button("Обновить категорию");
        updateCategoryButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        updateCategoryButton.setIcon(VaadinIcon.EDIT.create());
        updateCategoryButton.addClickListener(event -> {
            Select<QuestionCategoryEntity> select = new Select<>();
            select.setPlaceholder("Тема");
            select.setItems(questionService.findAllCategories());
            select.setRenderer(new ComponentRenderer<Component, QuestionCategoryEntity>(
                    category -> new Span(category.getName())));

            QuizComponents.openConfirmDialog(
                    select,
                    "Выберите тему",
                    () -> {
                        questionService.updateCategory(grid.getSelectedItems(), select.getValue());
                        grid.asMultiSelect().deselectAll();
                        updateListAsync();
                        groupedOperations.setVisible(false);
                    });
        });

        Button updateOptionsOnly = new Button("Поменять механику");
        updateOptionsOnly.setTooltipText("Вопросы которые можно задать только с вариантами становятся сингл и наоборот." +
                "По усмотрению автора");
        updateOptionsOnly.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        updateOptionsOnly.setIcon(VaadinIcon.OPTIONS.create());
        updateOptionsOnly.addClickListener(event -> {
            questionService.updateOptionsOnlyProperty(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
            updateListAsync();
            groupedOperations.setVisible(false);
        });

        groupedOperations.add(deleteAllButton, updateCategoryButton, updateOptionsOnly);
    }

    private void editQuestion(FourAnswersQuestionBindingModel model) {
        if (model == null) {
            formDialog.close();
            form.setModel(null);
        } else {
            addToDialogAndOpen(form);
            form.setCategoryList(questionService.findAllCategories());
            form.setModel(model);
        }
    }

    private void addToDialogAndOpen(Component component) {
        formDialog.removeAll();
        formDialog.add(component);
        formDialog.open();
    }

    private void updateListByFilter(Predicate<? super QuestionEntity> filterCondition) {
        grid.setQuestions(questionService.findAllCreatedByCurrentUser().stream()
                .filter(filterCondition)
                .collect(Collectors.toList()));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        updateListAsync();
    }
}
