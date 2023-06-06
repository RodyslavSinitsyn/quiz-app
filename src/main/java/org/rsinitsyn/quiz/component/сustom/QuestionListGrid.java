package org.rsinitsyn.quiz.component.сustom;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionGrade;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.QuizUtils;

@Slf4j
public class QuestionListGrid extends Grid<QuestionEntity> {

    public QuestionListGrid(List<QuestionEntity> questionEntityList, boolean defaultHeight) {
        setQuestions(questionEntityList);
        if (defaultHeight) {
            setHeight("40em");
        }
//        setAllRowsVisible(true);
    }

    public QuestionListGrid(List<QuestionEntity> questionEntityList) {
        this(questionEntityList, false);
    }

    public void setQuestions(List<QuestionEntity> questions) {
        setItems(DataProvider.fromCallbacks(
                query -> {
                    log.trace("offset: {}, limit: {}, page: {}, pageSize: {}",
                            query.getOffset(), query.getLimit(), query.getPage(), query.getPageSize());
                    Stream<QuestionEntity> stream = questions.stream();
                    if (query.getSortingComparator().isPresent()) {
                        stream = stream.sorted(query.getSortingComparator().get());
                    }
                    return stream
                            .skip(query.getOffset())
                            .limit(query.getLimit());
                },
                query -> questions.size()
        ));
    }

    public void addDefaultColumns() {
        addTextColumn(5);
        addCategoryColumn();
        addColumn(QuestionEntity::getCreatedBy)
                .setHeader("Автор")
                .setSortable(true);
        addMechanicColumn();
        addColumn(new ComponentRenderer<>(entity -> {
            int size = entity.getGrades().size();
            if (size == 0) {
                return VaadinIcon.MINUS_CIRCLE_O.create();
            }
            double gradeVal = entity.getGradeValue();
            return new Span(
                    new Span(gradeVal + " "),
                    VaadinIcon.STAR_O.create(),
                    new Span(" (" + size + ")")
            );
        }))
                .setHeader("Сложность")
                .setTooltipGenerator(entity -> entity.getGrades().stream()
                        .map(qg -> qg.getUser().getUsername() + " - " + qg.getGrade())
                        .collect(Collectors.joining(System.lineSeparator())))
                .setSortable(true)
                .setComparator(Comparator.comparingDouble(QuestionEntity::getGradeValue).thenComparingInt(q -> q.getGrades().size()));
        addColumn(new LocalDateTimeRenderer<>(QuestionEntity::getCreationDate, QuizUtils.DATE_FORMAT_VALUE))
                .setHeader("Дата создания")
                .setSortable(true)
                .setComparator(Comparator.comparing(QuestionEntity::getCreationDate));
        addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    }

    public void addTextColumn(int flexGrow) {
        addColumn(new ComponentRenderer<>(QuizComponents::questionDescription))
                .setHeader("Текст")
                .setFlexGrow(flexGrow)
                .setTooltipGenerator(entity -> entity.getAnswers().stream()
                        .filter(AnswerEntity::isCorrect)
                        .map(AnswerEntity::getText)
                        .collect(Collectors.joining(", "))
                );
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
