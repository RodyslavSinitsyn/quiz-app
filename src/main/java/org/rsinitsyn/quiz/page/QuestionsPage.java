package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
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
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.custom.QuestionListGrid;
import org.rsinitsyn.quiz.component.custom.form.*;
import org.rsinitsyn.quiz.entity.QuestionCategoryEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.*;
import org.rsinitsyn.quiz.service.ImportService;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.rsinitsyn.quiz.service.QuestionService;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.ModelConverterUtils;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Route(value = "/list", layout = MainLayout.class)
@PageTitle("Questions")
@PermitAll
public class QuestionsPage extends VerticalLayout implements AfterNavigationObserver {

    private QuestionListGrid grid;
    private TextField filterText = new TextField();
    private MultiSelectComboBox<QuestionCategoryEntity> categoryFilter = new MultiSelectComboBox<>();
    private HorizontalLayout groupedOperations = new HorizontalLayout();
    private Span spinner = new Span("Loading...");

    private Dialog formDialog;
    private QuestionCategoryForm categoryForm;
    private AbstractQuestionCreationForm<FourAnswersQuestionBindingModel> form;
    private AbstractQuestionCreationForm<PhotoQuestionBindingModel> photoForm;
    private AbstractQuestionCreationForm<PrecisionQuestionBindingModel> precisionForm;
    private AbstractQuestionCreationForm<OrQuestionBindingModel> orForm;
    private AbstractQuestionCreationForm<TopQuestionBindingModel> topForm;
    private AbstractQuestionCreationForm<LinkQuestionBindingModel> linkForm;

    private final QuestionService questionService;
    private final ImportService importService;
    private final UserService userService;
    private final QuestionCategoryService categoryService;
    // Async workaround, move to separate service
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Executor securityDelegatingExecutor = new DelegatingSecurityContextExecutor(executor);

    public QuestionsPage(QuestionService questionService,
                         ImportService importService,
                         UserService userService,
                         QuestionCategoryService categoryService) {
        this.questionService = questionService;
        this.importService = importService;
        this.userService = userService;
        this.categoryService = categoryService;

        configureGrid();
        configureForms();
        configureCategoryForm();
        configureDialog();

        add(QuizComponents.mainHeader("База вопросов"),
                createToolbar(),
                spinner,
                grid);
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
            } else if (event.getItem().getType().equals(QuestionType.TOP)) {
                topForm.setModel(ModelConverterUtils.toTopQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(topForm);
            } else if (event.getItem().getType().equals(QuestionType.PHOTO)) {
                photoForm.setModel(ModelConverterUtils.toPhotoQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(photoForm);
            } else if (event.getItem().getType().equals(QuestionType.LINK)) {
                linkForm.setModel(ModelConverterUtils.toLinkQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(linkForm);
            } else {
                form.setModel(ModelConverterUtils.toFourAnswersQuestionBindingModel(event.getItem()));
                addToDialogAndOpen(form);
            }
        });
    }

    @SuppressWarnings("all")
    private void updateListAsync() {
        spinner.setVisible(true);
        UI ui = getUI().orElse(null);
        CompletableFuture.supplyAsync(() -> questionService.findAllCreatedByCurrentUser(), securityDelegatingExecutor)
                .thenAccept(questionEntities -> ui.access(() -> {
                    grid.setQuestions(questionEntities);
                    spinner.setVisible(false);
                }));
    }

    private void configureForms() {
        var categories = categoryService.findAllCategories();
        form = new QuestionForm(categories, userService.findAllOrderByVisitDateDesc());
        photoForm = new PhotoQuestionForm(categories);
        precisionForm = new PrecisionQuestionForm(categories);
        orForm = new OrQuestionForm(categories);
        topForm = new TopQuestionForm(categories);
        linkForm = new LinkQuestionForm(categories);
        configureAbstractQuestionFormsDefault(form, photoForm, precisionForm, orForm, topForm, linkForm);
    }

    @SafeVarargs
    private void configureAbstractQuestionFormsDefault(AbstractQuestionCreationForm<? extends AbstractQuestionBindingModel>... forms) {
        for (AbstractQuestionCreationForm<? extends AbstractQuestionBindingModel> form : forms) {
            form.addSaveEventListener(event -> {
                questionService.saveOrUpdate(event.getModel());
                updateListAsync();
                formDialog.close();
            });
            form.addDeleteEventListener(event -> {
                questionService.deleteById(event.getModel().getId());
                updateListAsync();
                formDialog.close();
                grid.asMultiSelect().clear();
            });
            form.addCancelEventListener(event -> formDialog.close());
            form.setModel(null);
        }
    }

    private void configureCategoryForm() {
        categoryForm = new QuestionCategoryForm(categoryService.findAllCategories(), new QuestionCategoryBindingModel());
        categoryForm.setWidth("30em");
        categoryForm.addListener(QuestionCategoryForm.SaveCategoryEvent.class, event -> {
            categoryService.save(event.getModel().getCategoryName());
            formDialog.close();
            categoryForm.setModel(null);
            categoryForm.setCategories(categoryService.findAllCategories());
            categoryFilter.setItems(categoryService.findAllCategories());
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
        categoryFilter.setItems(categoryService.findAllCategories());
        categoryFilter.setItemLabelGenerator(item -> item.getName());
        categoryFilter.addSelectionListener(event -> {
            if (event.getValue().isEmpty()) {
                updateListAsync();
            } else {
                updateListByFilter(entity -> event.getValue().contains(entity.getCategory()));
            }
        });

        Button addQuestionButton = createButton("Вопрос", event -> {
            grid.asMultiSelect().clear();
            form.setModel(new FourAnswersQuestionBindingModel());
            addToDialogAndOpen(form);
        });

        Button addPhotoQuestionButton = createButton("Фото", event -> {
            grid.asMultiSelect().clear();
            photoForm.setModel(new PhotoQuestionBindingModel());
            addToDialogAndOpen(photoForm);
        });

        Button addPrecisionQuestionButton = createButton("Точный", event -> {
            grid.asMultiSelect().clear();
            precisionForm.setModel(new PrecisionQuestionBindingModel());
            addToDialogAndOpen(precisionForm);
        });

        Button addOrQuestionButton = createButton("Или", event -> {
            grid.asMultiSelect().clear();
            orForm.setModel(new OrQuestionBindingModel());
            addToDialogAndOpen(orForm);
        });

        Button addTopQuestionButton = createButton("Топ", event -> {
            grid.asMultiSelect().clear();
            topForm.setModel(new TopQuestionBindingModel());
            addToDialogAndOpen(topForm);
        });

        Button addLinkQuestionButton = createButton("Матч", event -> {
            grid.asMultiSelect().clear();
            linkForm.setModel(new LinkQuestionBindingModel());
            addToDialogAndOpen(linkForm);
        });

        Upload uploadComponent = QuizComponents.uploadComponent(
                "Импортировать",
                (buffer, event) -> {
                    InputStream inputStream = buffer.getInputStream();
                    importService.importQuestions(inputStream);
                    updateListAsync();
                }, ".txt");

        Button addCategoryButton = createButton("Добавить тему", event -> {
            categoryForm.setModel(new QuestionCategoryBindingModel());
            addToDialogAndOpen(categoryForm);
        });

        configureGroupedActions();

        HorizontalLayout toolbar = new HorizontalLayout(
                categoryFilter,
                addQuestionButton,
                addPhotoQuestionButton,
                addPrecisionQuestionButton,
                addOrQuestionButton,
                addTopQuestionButton,
                addLinkQuestionButton,
                uploadComponent,
                addCategoryButton,
                groupedOperations);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);
        toolbar.setJustifyContentMode(JustifyContentMode.CENTER);
        return toolbar;
    }

    private Button createButton(String text, ComponentEventListener<ClickEvent<Button>> eventHandler) {
        Button button = new Button(text);
        button.addThemeVariants(ButtonVariant.LUMO_SMALL,
                ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(eventHandler);
        return button;
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

        Button updateCategoryButton = new Button("Обновить тему");
        updateCategoryButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        updateCategoryButton.setIcon(VaadinIcon.EDIT.create());
        updateCategoryButton.addClickListener(event -> {
            Select<QuestionCategoryEntity> select = new Select<>();
            select.setPlaceholder("Тема");
            select.setItems(categoryService.findAllCategories());
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
