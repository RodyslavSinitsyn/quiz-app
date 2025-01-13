package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import lombok.Getter;
import org.rsinitsyn.quiz.component.custom.QuestionListGrid;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.utils.QuizComponents;

public class CleverestGameSettingsComponent extends VerticalLayout {

    private List<QuestionEntity> questionEntityList;

    private final QuestionListGrid allQuestionsGrid = new QuestionListGrid(Collections.emptyList(), true);
    private final QuestionListGrid firstRoundGrid = new QuestionListGrid(Collections.emptyList());
    private final QuestionListGrid secondRoundGrid = new QuestionListGrid(Collections.emptyList());
    private final QuestionListGrid thirdRoundGrid = new QuestionListGrid(Collections.emptyList());

    private Span helpSettingsText = new Span();
    private HorizontalLayout buttonsLayout = new HorizontalLayout();
    private Button submitButton = new Button();

    // drag props
    private QuestionEntity draggedQuestion;
    private QuestionListGrid draggedInitiatedFromGrid;
    private boolean dragOntoSelf = false;

    // filter
    private boolean hiddenFilterUsed = false;

    public CleverestGameSettingsComponent(List<QuestionEntity> questionEntityList) {
        this.questionEntityList = questionEntityList;

        configureToolbar();
        configureButtons();
        configureGrids();
        updateHelpText();

        add(QuizComponents.mainHeader("Список вопросов"));
        add(buttonsLayout);
        add(allQuestionsGrid);
        add(createRoundGridsLayout());
        add(submitButton);
    }

    public void setQuestions(List<QuestionEntity> questions) {
        this.questionEntityList = questions;
        this.allQuestionsGrid.setQuestions(questions);
    }

    private void configureToolbar() {
        Button first = new Button("Первый раунд");
        first.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        first.addClickShortcut(Key.DIGIT_1);
        first.addClickListener(event -> {
            firstRoundGrid.getListDataView().addItems(new ArrayList<>(allQuestionsGrid.getSelectedItems()));
            allQuestionsGrid.asMultiSelect().deselectAll();
            updateHelpText();
        });

        Button second = new Button("Второй раунд");
        second.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        second.addClickShortcut(Key.DIGIT_2);
        second.addClickListener(event -> {
            secondRoundGrid.getListDataView().addItems(new ArrayList<>(allQuestionsGrid.getSelectedItems()));
            allQuestionsGrid.asMultiSelect().deselectAll();
            updateHelpText();
        });

        Button third = new Button("Третий раунд");
        third.addClickShortcut(Key.DIGIT_3);
        third.addClickListener(event -> {
            thirdRoundGrid.getListDataView().addItems(new ArrayList<>(allQuestionsGrid.getSelectedItems()));
            allQuestionsGrid.asMultiSelect().deselectAll();
            updateHelpText();
        });

        Button remove = new Button("Убрать из раундов");
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        remove.addClickShortcut(Key.BACKSPACE);
        remove.addClickListener(event -> {
            firstRoundGrid.getListDataView().removeItems(allQuestionsGrid.getSelectedItems());
            secondRoundGrid.getListDataView().removeItems(allQuestionsGrid.getSelectedItems());
            thirdRoundGrid.getListDataView().removeItems(allQuestionsGrid.getSelectedItems());

            updateHelpText();
            allQuestionsGrid.asMultiSelect().deselectAll();
        });

        Button hideUsed = new Button("Убрать" + " used");
        hideUsed.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
        hideUsed.addClickListener(event -> {
            hiddenFilterUsed = !hiddenFilterUsed;

            allQuestionsGrid.setItems(
                    hiddenFilterUsed
                            ? questionEntityList.stream()
                            .filter(question -> !question.presentInAnyGame())
                            .toList()
                            : questionEntityList);

            event.getSource().setText((hiddenFilterUsed ? "Вернуть" : "Убрать") + " used");
        });

        buttonsLayout.setWidthFull();
        buttonsLayout.setAlignItems(Alignment.CENTER);
        buttonsLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        buttonsLayout.add(first, second, third, remove, hideUsed, helpSettingsText);
    }


    private void updateHelpText() {
        helpSettingsText.setText(
                String.format("первый[%d] второй[%d] третий[%d]",
                        firstRoundGrid.getListDataView().getItemCount(),
                        secondRoundGrid.getListDataView().getItemCount(),
                        thirdRoundGrid.getListDataView().getItemCount())
        );
    }

    private HorizontalLayout createRoundGridsLayout() {
        var layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setAlignItems(Alignment.STRETCH);
        layout.setJustifyContentMode(JustifyContentMode.AROUND);
        layout.add(new VerticalLayout() {{
            add(QuizComponents.subHeader("Раунд 1"), firstRoundGrid);
        }});
        layout.add(new VerticalLayout() {{
            add(QuizComponents.subHeader("Раунд 2"), secondRoundGrid);
        }});
        layout.add(new VerticalLayout() {{
            add(QuizComponents.subHeader("Раунд 3"), thirdRoundGrid);
        }});
        return layout;
    }

    private void configureGrids() {
        allQuestionsGrid.addColumn(new ComponentRenderer<>(QuizComponents::questionLinkedWithGameIcon))
                .setHeader("Связь")
                .setFlexGrow(0);
        allQuestionsGrid.addColumn(entity -> {
                    boolean firstRoundQ = firstRoundGrid.getListDataView().contains(entity);
                    boolean secondRoundQ = secondRoundGrid.getListDataView().contains(entity);
                    boolean thirdRoundQ = thirdRoundGrid.getListDataView().contains(entity);

                    boolean inManyRounds = Stream.of(firstRoundQ, secondRoundQ, thirdRoundQ)
                            .filter(Boolean::booleanValue)
                            .count() > 1;

                    String msg = "-";
                    if (inManyRounds) {
                        msg = "!НЕСКОЛЬКО!";
                    } else if (firstRoundQ) {
                        msg = "1 раунд";
                    } else if (secondRoundQ) {
                        msg = "2 раунд";
                    } else if (thirdRoundQ) {
                        msg = "3 раунд";
                    }
                    return msg;
                })
                .setHeader("Раунд")
                .setSortable(true)
                .setFlexGrow(0);
        allQuestionsGrid.addDefaultColumns();
        allQuestionsGrid.setItems(questionEntityList);
        allQuestionsGrid.setSelectionMode(Grid.SelectionMode.MULTI);
        allQuestionsGrid.addItemClickListener(event -> allQuestionsGrid.select(event.getItem()));

        configureRoundGrid(firstRoundGrid);
        configureRoundGrid(secondRoundGrid);
        configureRoundGrid(thirdRoundGrid);
    }

    private void configureRoundGrid(QuestionListGrid grid) {
        var gridDataView = grid.setItems(new ArrayList<>());

        grid.setWidthFull();
        grid.setAllRowsVisible(true);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid.addTextColumn(5);
        grid.addCategoryColumn();
        grid.addMechanicColumn();
        grid.addItemDoubleClickListener(event -> {
            gridDataView.removeItem(event.getItem());
            allQuestionsGrid.getDataProvider().refreshAll();
            updateHelpText();
        });
        grid.setDropMode(GridDropMode.BETWEEN);
        grid.setRowsDraggable(true);
        grid.addDragStartListener(event -> {
            dragOntoSelf = false;
            draggedQuestion = event.getDraggedItems().get(0);
            draggedInitiatedFromGrid = grid;
        });
        grid.addDragEndListener(event -> {
            if (!dragOntoSelf) {
                gridDataView.removeItem(draggedQuestion);
                allQuestionsGrid.getDataProvider().refreshAll();
                updateHelpText();
            }
            draggedQuestion = null;
        });
        grid.addDropListener(event -> {
            if (draggedInitiatedFromGrid == grid) {
                dragOntoSelf = true;
            }
            var targetQuestion = event.getDropTargetItem().orElse(null);
            gridDataView.removeItem(draggedQuestion);
            if (targetQuestion == null) {
                gridDataView.addItem(draggedQuestion);
                return;
            }
            if (event.getDropLocation() == GridDropLocation.BELOW) {
                gridDataView.addItemAfter(draggedQuestion, targetQuestion);
            } else {
                gridDataView.addItemBefore(draggedQuestion, targetQuestion);
            }
        });
    }

    private void configureButtons() {
        submitButton.setText("Подтвердить вопросы");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.addClickListener(event -> {
            fireEvent(new SettingsCompletedEvent(this,
                    firstRoundGrid.getListDataView().getItems().toList(),
                    secondRoundGrid.getListDataView().getItems().toList(),
                    thirdRoundGrid.getListDataView().getItems().toList()));
        });
    }

    @Getter
    public static class SettingsCompletedEvent extends ComponentEvent<CleverestGameSettingsComponent> {

        private List<QuestionEntity> firstRound;
        private List<QuestionEntity> secondRound;
        private List<QuestionEntity> thirdRound;

        public SettingsCompletedEvent(CleverestGameSettingsComponent source,
                                      List<QuestionEntity> firstRound,
                                      List<QuestionEntity> secondRound,
                                      List<QuestionEntity> thirdRound) {
            super(source, false);
            this.firstRound = firstRound;
            this.secondRound = secondRound;
            this.thirdRound = thirdRound;
        }
    }

    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
