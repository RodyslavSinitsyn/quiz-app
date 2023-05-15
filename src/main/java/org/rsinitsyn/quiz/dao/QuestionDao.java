package org.rsinitsyn.quiz.dao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QuestionDao extends JpaRepository<QuestionEntity, UUID> {
    @Query("select distinct q from QuestionEntity q " +
            "join fetch q.category " +
            "join fetch q.answers " +
            "order by q.creationDate desc ")
    List<QuestionEntity> findAllJoinAnswersAndCategoryNewFirst();

    List<QuestionEntity> findAllByCreatedBy(String createdBy);

    @Query("SELECT qe FROM QuestionEntity qe JOIN FETCH qe.answers WHERE qe.id = :id")
    Optional<QuestionEntity> findByIdJoinAnswers(@Param("id") UUID id);

    List<QuestionEntity> findAllByIdIn(Iterable<UUID> questionIds);

    @Transactional
    int deleteQuestionEntityById(@Param("id") UUID id);
}
