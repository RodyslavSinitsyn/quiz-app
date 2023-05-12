package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.QuestionGrade;
import org.rsinitsyn.quiz.entity.QuestionGradePrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionGradeDao extends JpaRepository<QuestionGrade, QuestionGradePrimaryKey> {
    @Query("FROM QuestionGrade qg JOIN FETCH qg.question WHERE qg.questionId = :questionId")
    List<QuestionGrade> findAllByQuestionId(@Param("questionId") UUID questionId);
}
