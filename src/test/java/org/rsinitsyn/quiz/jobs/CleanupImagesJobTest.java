package org.rsinitsyn.quiz.jobs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.dao.QuestionHintDao;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.AnswerEntity;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionHintEntity;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.ResourceService;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class CleanupImagesJobTest {

    private UserDao userDao;
    private QuestionDao questionDao;
    private QuestionHintDao questionHintDao;
    private ResourceService resourceService;
    private QuizAppProperties properties;
    private FilesProvider filesProvider;

    private CleanupImagesJob cleanupImagesJob;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        questionDao = mock(QuestionDao.class);
        questionHintDao = mock(QuestionHintDao.class);
        resourceService = mock(ResourceService.class);
        properties = mock(QuizAppProperties.class);
        filesProvider = mock(FilesProvider.class);

        cleanupImagesJob = new CleanupImagesJob(
                userDao,
                questionDao,
                questionHintDao,
                resourceService,
                properties,
                filesProvider
        );
    }

    @Test
    void deletes_only_orphan_files_given_linked_files_from_all_entities() throws Exception {
        given(properties.getFilesFolder()).willReturn("test/");

        final var user = new UserEntity();
        user.setPhotoFilename("test/user.png");

        final var answer = new AnswerEntity();
        answer.setPhotoFilename("test/answer.png");

        final var question = new QuestionEntity();
        question.setPhotoFilename("test/question.png");
        question.setAnswers(List.of(answer));

        final var hint = new QuestionHintEntity();
        hint.setPhotoFilename("test/hint.png");

        given(userDao.findAll()).willReturn(List.of(user));
        given(questionDao.findAll()).willReturn(List.of(question));
        given(questionHintDao.findAll()).willReturn(List.of(hint));

        given(filesProvider.getFiles(any())).willReturn(Set.of(
                Path.of("user.png"),
                Path.of("question.png"),
                Path.of("answer.png"),
                Path.of("hint.png"),
                Path.of("orphan.png")
        ));

        cleanupImagesJob.run();

        verify(resourceService).deleteImageFile("test/orphan.png");

        verify(resourceService, never()).deleteImageFile("test/user.png");
        verify(resourceService, never()).deleteImageFile("test/question.png");
        verify(resourceService, never()).deleteImageFile("test/answer.png");
        verify(resourceService, never()).deleteImageFile("test/hint.png");
    }
}