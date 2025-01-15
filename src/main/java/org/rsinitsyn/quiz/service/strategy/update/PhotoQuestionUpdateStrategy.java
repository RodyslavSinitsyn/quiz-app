package org.rsinitsyn.quiz.service.strategy.update;

import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;
import org.rsinitsyn.quiz.model.binding.PhotoQuestionBindingModel;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.QuestionCategoryService;
import org.springframework.stereotype.Service;

@Service
public class PhotoQuestionUpdateStrategy extends AbstractQuestionUpdateStrategy<PhotoQuestionBindingModel> {


    public PhotoQuestionUpdateStrategy(QuizAppProperties properties,
                                       QuestionCategoryService categoryService) {
        super(properties, categoryService);
    }

    @Override
    public void setType(PhotoQuestionBindingModel model, QuestionEntity question) {
        question.setType(QuestionType.PHOTO);
    }

    @Override
    protected void createHook(PhotoQuestionBindingModel model, QuestionEntity question) {
        super.createHook(model, question);
        appendAnswers(model, question);
    }

    @Override
    protected void updateHook(PhotoQuestionBindingModel model, QuestionEntity question, QuestionEntity persistEntity) {
        super.updateHook(model, question, persistEntity);
        question.getAnswers().clear();
        appendAnswers(model, question);
    }

    private void appendAnswers(PhotoQuestionBindingModel model, QuestionEntity question) {
        question.addAnswer(createAnswerEntity("A", true, 0, model.getCorrectOption()));
        question.addAnswer(createAnswerEntity("B", false, 1, model.getOptionTwo()));
        question.addAnswer(createAnswerEntity("C", false, 2, model.getOptionThree()));
        question.addAnswer(createAnswerEntity("D", false, 3, model.getOptionFour()));
    }

    @Override
    public String getBindingModelClassName() {
        return PhotoQuestionBindingModel.class.getSimpleName();
    }
}
