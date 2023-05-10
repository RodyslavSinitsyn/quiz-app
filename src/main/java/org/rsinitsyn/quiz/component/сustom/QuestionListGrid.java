package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.Comparator;
import java.util.List;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

public class QuestionListGrid extends Grid<QuestionEntity> {

    public QuestionListGrid(List<QuestionEntity> questionEntityList) {
        setItems(questionEntityList);
    }

    public void addDefaultColumns() {
        addTextColumn(5);
        addCategoryColumn();
        addColumn(QuestionEntity::getCreatedBy)
                .setHeader("Автор")
                .setSortable(true);
        addMechanicColumn();
        addColumn(new LocalDateTimeRenderer<>(QuestionEntity::getCreationDate, QuizUtils.DATE_FORMAT_VALUE))
                .setHeader("Дата создания")
                .setSortable(true)
                .setComparator(Comparator.comparing(QuestionEntity::getCreationDate));
        addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        setAllRowsVisible(true);
    }

    public void addTextColumn(int flexGrow) {
        addColumn(new ComponentRenderer<>(QuizComponents::questionDescription))
                .setHeader("Текст")
                .setFlexGrow(flexGrow);
    }

    public void addCategoryColumn() {
        addColumn(entity -> entity.getCategory().getName())
                .setHeader("Тема")
                .setSortable(true);
    }

    public void addMechanicColumn() {
        addColumn(new ComponentRenderer<>(QuizComponents::questionMechanicSpan))
                .setHeader("Механика")
                .setTooltipGenerator(entity -> entity.isOptionsOnly()
                        ? "Вопрос задается только с вариантами"
                        : "Вопрос может быть задан без вариантов");
    }
}
