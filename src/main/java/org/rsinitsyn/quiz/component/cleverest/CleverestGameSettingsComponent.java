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
import java.util.stream.Stream;
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
    private Set<QuestionEntity> thirdRoundQuestions = new HashSet<>();
    private Set<QuestionEntity> specialQuestions = new HashSet<>();

    public CleverestGameSettingsComponent(List<QuestionEntity> questionEntityList) {
        this.questionEntityList = questionEntityList;

        configureToolbar();
        configureButtons();
        configureGrid();

        add(new H3("Выберите вопросы!"));
        add(buttons);
        add(grid);
        add(submitButton);
    }

    private void configureToolbar() {
        Button first = new Button("Первый раунд");
        first.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        first.addClickListener(event -> {
            firstRoundQuestions.addAll(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
        });

        Button second = new Button("Второй раунд");
        second.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        second.addClickListener(event -> {
            secondRoundQuestions.addAll(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
        });

        Button third = new Button("Третий раунд");
        third.addClickListener(event -> {
            thirdRoundQuestions.addAll(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
        });

        Button special = new Button("Спец");
        special.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);
        special.addClickListener(event -> {
            specialQuestions.addAll(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
        });

        Button remove = new Button("Убрать из раундов");
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        remove.addClickListener(event -> {
            firstRoundQuestions.removeAll(grid.getSelectedItems());
            secondRoundQuestions.removeAll(grid.getSelectedItems());
            thirdRoundQuestions.removeAll(grid.getSelectedItems());
            specialQuestions.removeAll(grid.getSelectedItems());
            grid.asMultiSelect().deselectAll();
        });
//        Button toBottom = new Button("Вниз");
//        toBottom.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
//        toBottom.addClickListener(event -> grid.scrollToIndex(40));

        buttons.add(first, second, third, special, remove);
    }

    private void configureGrid() {
        grid.addColumn(entity -> {
                    boolean firstRoundQ = firstRoundQuestions.stream().anyMatch(e -> e.getId().equals(entity.getId()));
                    boolean secondRoundQ = secondRoundQuestions.stream().anyMatch(e -> e.getId().equals(entity.getId()));
                    boolean thirdRoundQ = thirdRoundQuestions.stream().anyMatch(e -> e.getId().equals(entity.getId()));
                    boolean specialQ = specialQuestions.stream().anyMatch(e -> e.getId().equals(entity.getId()));

                    boolean inManyRounds = Stream.of(firstRoundQ, secondRoundQ, thirdRoundQ, specialQ)
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
                    } else if (specialQ) {
                        msg = "Спец";
                    }
                    return msg;
                })
                .setHeader("Раунд")
                .setSortable(true)
                .setFlexGrow(0);
        grid.addDefaultColumns();
        grid.setItems(questionEntityList);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addItemClickListener(event -> grid.select(event.getItem()));
    }

    private void configureButtons() {
        submitButton.setText("Подтвердить вопросы");
        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        submitButton.addClickListener(event -> {
            fireEvent(new SettingsCompletedEvent(this,
                    firstRoundQuestions,
                    secondRoundQuestions,
                    thirdRoundQuestions,
                    specialQuestions));
        });
    }

    @Getter
    public static class SettingsCompletedEvent extends ComponentEvent<CleverestGameSettingsComponent> {

        private Set<QuestionEntity> firstRound;
        private Set<QuestionEntity> secondRound;
        private Set<QuestionEntity> thirdRound;
        private Set<QuestionEntity> special;

        public SettingsCompletedEvent(CleverestGameSettingsComponent source,
                                      Set<QuestionEntity> firstRound,
                                      Set<QuestionEntity> secondRound, Set<QuestionEntity> thirdRound, Set<QuestionEntity> special) {
            super(source, false);
            this.firstRound = firstRound;
            this.secondRound = secondRound;
            this.thirdRound = thirdRound;
            this.special = special;
        }
    }


    public <T extends ComponentEvent<?>> Registration addListener(Class<T> eventType,
                                                                  ComponentEventListener<T> listener) {
        return getEventBus().addListener(eventType, listener);
    }
}