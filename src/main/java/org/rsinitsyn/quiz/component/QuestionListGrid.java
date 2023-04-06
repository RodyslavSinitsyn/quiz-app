package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class QuestionListGrid extends Grid<QuestionEntity> {

    public QuestionListGrid(List<QuestionEntity> questionEntityList) {
        setItems(questionEntityList);
        configure();
    }

    private void configure() {
        addColumn(new ComponentRenderer<>(entity -> {
            HorizontalLayout row = new HorizontalLayout();
            if (!entity.getGameQuestions().isEmpty()) {
                Icon icon = VaadinIcon.LINK.create();
                icon.setTooltipText("Вопрос связан с игрой и не может быть удален");
                row.add(icon);
            }
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            if (StringUtils.isNotEmpty(entity.getPhotoFilename())) {
                Avatar smallPhoto = new Avatar();
                smallPhoto.setImageResource(
                        QuizUtils.createStreamResourceForPhoto(entity.getPhotoFilename()));
                row.add(smallPhoto);
            }
            row.add(new Span(
                    entity.getText().length() > 300
                            ? entity.getText().substring(0, 300).concat("...")
                            : entity.getText()));
            return row;
        }))
                .setHeader("Текст")
                .setFlexGrow(5);
        addColumn(QuestionEntity::getCreatedBy)
                .setHeader("Автор")
                .setSortable(true);
        addColumn(entity -> entity.getCategory().getName())
                .setHeader("Тема")
                .setSortable(true);
        addColumn(new ComponentRenderer<>(QuizComponents::questionMechanicSpan))
                .setHeader("Механика вопроса")
                .setTooltipGenerator(entity -> entity.isOptionsOnly()
                        ? "Вопрос задается только с вариантами"
                        : "Вопрос может быть задан без вариантов");
        addColumn(new LocalDateTimeRenderer<>(QuestionEntity::getCreationDate, QuizUtils.DATE_FORMAT_VALUE))
                .setHeader("Дата создания")
                .setSortable(true)
                .setComparator(Comparator.comparing(QuestionEntity::getCreationDate));
        addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        setAllRowsVisible(true);
    }
}
