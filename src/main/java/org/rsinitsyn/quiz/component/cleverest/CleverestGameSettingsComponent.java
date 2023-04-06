package org.rsinitsyn.quiz.component.cleverest;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.shared.Registration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.rsinitsyn.quiz.component.QuestionListGrid;
import org.rsinitsyn.quiz.entity.QuestionEntity;

public class CleverestGameSettingsComponent extends VerticalLayout {

    private QuestionListGrid grid = new QuestionListGrid(Collections.emptyList());
    private HorizontalLayout buttons = new HorizontalLayout();
    private Button submitButton = new Button();

    private List<QuestionEntity> questionEntityList;
    private Set<QuestionEntity> firstRoundQuestions = new HashSet<>();
    private Set<QuestionEntity> secondRoundQuestions = new HashSet<>();

    public CleverestGameSettingsComponent(List<QuestionEntity> questionEntityList) {
        this.questionEntityList = questionEntityList;

        configureToolbar();
        configureGrid();
        configureButtons();

        add(new H3("Выберите вопросы!"));
        add(buttons);
        add(grid);
        add(submitButton);
    }

    private void configureToolbar() {
        Button first = new Button("Первый раунд");
        first.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        first.addClickListener(event -> {
            firstRoundQuestions = grid.getSelectedItems();
            grid.asMultiSelect().deselectAll();
        });

        Button second = new Button("Второй раунд");
        second.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        second.addClickListener(event -> {
            secondRoundQuestions = grid.getSelectedItems();
            grid.asMultiSelect().deselectAll();
        });

        buttons.add(first, second);
    }

    private void configureGrid() {
        grid.addColumn(entity -> {
                    boolean existsInFirstList = firstRoundQuestions.stream().anyMatch(e -> e.getId().equals(entity.getId()));
                    boolean existsInSecondList = secondRoundQuestions.stream().anyMatch(e -> e.getId().equals(entity.getId()));
                    String msg = "Не выбран";
                    if (existsInFirstList) {
                        msg = "Раунд 1";
                    } else if (existsInSecondList) {
                        msg = "Раунд 2";
                    }
                    return msg;
                })
                .setHeader("Выбран");
        grid.setItems(questionEntityList);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addItemClickListener(event -> grid.select(event.getItem()));
    }

    private void configureButtons() {
        submitButton.setText("Подтвердить вопросы");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.addClickListener(event -> {
            fireEvent(new SettingsCompletedEvent(this, firstRoundQuestions, secondRoundQuestions));
        });
    }

    @Getter
    public static class SettingsCompletedEvent extends ComponentEvent<CleverestGameSettingsComponent> {

        private Set<QuestionEntity> firstRound;
        private Set<QuestionEntity> secondRound;

        public SettingsCompletedEvent(CleverestGameSettingsComponent source,
                                      Set<QuestionEntity> firstRound,
                                      Set<QuestionEntity> secondRound) {
            super(source, false);
            this.firstRound = firstRound;
            this.secondRound = secondRound;
        }
    }


    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}
