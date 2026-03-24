package org.rsinitsyn.quiz.service.strategy.update;

import org.apache.commons.io.FilenameUtils;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionHintEntity;
import org.rsinitsyn.quiz.model.binding.GuessPhotoQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.rsinitsyn.quiz.entity.QuestionHintType.PHOTO;
import static org.rsinitsyn.quiz.entity.QuestionType.GUESS_PHOTO;
import static org.rsinitsyn.quiz.utils.QuizUtils.generateFilenameWithExt;

@Service
public class GuessPhotoQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<GuessPhotoQuestionBindingModel> {

    public GuessPhotoQuestionUpdateStrategy(final QuizAppProperties properties, final QuestionCategoryService categoryService) {
        super(properties, categoryService);
    }

    @Override
    public void setType(final GuessPhotoQuestionBindingModel model, final QuestionEntity question) {
        question.setType(GUESS_PHOTO);
    }

    @Override
    protected void createHook(final GuessPhotoQuestionBindingModel model, final QuestionEntity question) {
        super.createHook(model, question);
        question.addAnswer(createAnswerEntity(model.getAnswerText(), true, 0, null));
    }

    @Override
    protected void updateHook(final GuessPhotoQuestionBindingModel model,
                              final QuestionEntity question,
                              final QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        persistEntity.getAnswers().stream().findFirst().orElseThrow().setText(model.getAnswerText());
    }

    @Override
    protected List<QuestionHintEntity> createHints(final GuessPhotoQuestionBindingModel model) {
        final var textHints = super.createHints(model);
        return Stream.concat(textHints.stream(), createPhotoHints(model)).toList();
    }

    private Stream<QuestionHintEntity> createPhotoHints(final GuessPhotoQuestionBindingModel model) {
        final var counter = new AtomicInteger();
        return model.getPhotoFilenameToResource().entrySet().stream()
                .map(entry -> {
                    final var entity = new QuestionHintEntity();
                    entity.setType(PHOTO);
                    final var extension = getExtension(entry.getKey());
                    entity.setText("");
                    entity.setOriginalPhotoUrl(entry.getKey());
                    entity.setPhotoFilename(properties.getFilesFolder() + generateFilenameWithExt(extension));
                    entity.setPhotoInputStream(entry.getValue());
                    entity.setNumber(counter.getAndIncrement());
                    return entity;
                });
    }

    @Override
    public String getBindingModelClassName() {
        return GuessPhotoQuestionBindingModel.class.getSimpleName();
    }
}
