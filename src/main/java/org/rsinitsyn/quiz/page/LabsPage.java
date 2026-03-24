package org.rsinitsyn.quiz.page;


import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.component.cleverest.CleverestComponents;
import org.rsinitsyn.quiz.component.custom.answer.AbstractAnswersLayout;
import org.rsinitsyn.quiz.component.custom.question.BaseQuestionLayout;
import org.rsinitsyn.quiz.component.custom.question.BaseQuestionLayout.QuestionAnsweredEvent;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.QuestionLayoutRequest;
import org.rsinitsyn.quiz.model.QuestionModel;
import org.rsinitsyn.quiz.service.QuestionService;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static com.vaadin.flow.component.notification.NotificationVariant.LUMO_CONTRAST;
import static java.util.UUID.randomUUID;
import static org.rsinitsyn.quiz.component.cleverest.CleverestComponents.notification;
import static org.rsinitsyn.quiz.component.custom.question.QuestionLayoutFactory.createQuestionLayout;

@Route(value = "/labs", layout = MainLayout.class)
@PageTitle("Labs")
@Slf4j
@PermitAll
public class LabsPage extends VerticalLayout {

    private final QuestionService questionService;

    public LabsPage(final QuestionService questionService) {
        this.questionService = questionService;

        final var fontSizes = new LinkedList<String>();
        fontSizes.add(LumoUtility.FontSize.XLARGE);
        fontSizes.add(LumoUtility.FontSize.XXLARGE);

        final var questions = questionService.findAllByCurrentUserAsModel();
        if (questions.isEmpty()) {
            renderMockQuestions(fontSizes);
            return;
        }

        for (final var question : questions) {
            final var sequenceQuestion = createQuestionLayout(new QuestionLayoutRequest()
                    .host(false)
                    .textClasses(List.of(LumoUtility.FontSize.XLARGE))
                    .question(question));
            add(sequenceQuestion);
            add(new Hr());

            sequenceQuestion.addListener(QuestionAnsweredEvent.class, e -> {
                final var event = e.getAnswerChosenEvent();
                final var text = "%s, %b, %d".formatted(
                        String.join(", ", event.getAnswers()),
                        event.isCorrect(),
                        0);
                notification(text, LUMO_CONTRAST);
            });
        }
    }

    private void renderMockQuestions(final LinkedList<String> fontSizes) {
        for (final var questionType : QuestionType.values()) {

            if (questionType == QuestionType.PHOTO) {
                continue;
            }

            final var sequenceQuestion = createQuestionLayout(new QuestionLayoutRequest()
                    .host(true)
                    .textClasses(List.of(Optional.ofNullable(fontSizes.poll())
                            .orElse(LumoUtility.FontSize.MEDIUM)))
                    .question(QuestionModel.builder()
                            .id(randomUUID())
                            .categoryName(questionType + " = " + randomText(2))
                            .text(randomText(10))
                            .type(questionType)
                            .answers(List.of(
                                    new QuestionModel.AnswerModel(randomText(1), true, 1, null),
                                    new QuestionModel.AnswerModel(randomText(7), true, 2, null),
                                    new QuestionModel.AnswerModel(randomText(20), true, 3, null),
                                    new QuestionModel.AnswerModel(randomText(35), true, 4, null)
                            ))
                            .build())
            );
            try {
                add(sequenceQuestion);
                add(new Hr());
            } catch (Exception e) {
            }
        }
    }

    public static String randomText(int wordCount) {
        final var generator = new RandomStringGenerator.Builder()
                .withinRange('a', 'z')
                .build();

        var builder = new StringBuilder();
        for (int i = 1; i <= wordCount; i++) {
            builder.append(generator.generate(5));
            if (i % 6 == 0) {      // после каждых 6 слов добавляем пробел
                builder.append(" ");
            } else if (i < wordCount) {
                builder.append(" ");  // обычный пробел между словами
            }
        }
        return builder.toString().trim();
    }
}
