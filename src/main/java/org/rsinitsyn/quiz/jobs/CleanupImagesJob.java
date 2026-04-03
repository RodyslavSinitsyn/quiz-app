package org.rsinitsyn.quiz.jobs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.dao.QuestionDao;
import org.rsinitsyn.quiz.dao.QuestionHintDao;
import org.rsinitsyn.quiz.dao.UserDao;
import org.rsinitsyn.quiz.entity.QuestionHintEntity;
import org.rsinitsyn.quiz.properties.QuizAppProperties;
import org.rsinitsyn.quiz.service.ResourceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.rsinitsyn.quiz.utils.QuizUtils.IMAGE_FOLDER;
import static org.rsinitsyn.quiz.utils.QuizUtils.RESOURCES_PATH;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupImagesJob {

    private final UserDao userDao;
    private final QuestionDao questionDao;
    private final QuestionHintDao questionHintDao;
    private final ResourceService resourceService;
    private final QuizAppProperties properties;
    private final FilesProvider filesProvider;

    @Transactional(readOnly = true)
    @Scheduled(initialDelay = 5_000, fixedDelay = 60_000)
    public void run() throws IOException {
        final var resourcesFolder = RESOURCES_PATH + IMAGE_FOLDER + properties.getFilesFolder();
        log.debug("Started images cleanup job. Folder: [{}]", resourcesFolder);
        final var diskFiles = filesProvider.getFiles(Path.of(resourcesFolder));
        final var fileReferences = loadLinkedFiles();
        diskFiles.forEach(r -> handle(fileReferences, r));
    }

    private void handle(Set<Reference> references, Path path) {
        final var fullPath = "%s%s".formatted(properties.getFilesFolder(), path.getFileName().toString());
        final var maybeReference = references.stream()
                .filter(r -> fullPath.contains(r.photoFilename))
                .findFirst();
        if (maybeReference.isEmpty()) {
            resourceService.deleteImageFile(fullPath);
        } else {
            log.debug("Image file [{}] linked to entity: [{}-{}]", path,
                    maybeReference.get().entity(),
                    maybeReference.get().id());
        }
    }

    private Set<Reference> loadLinkedFiles() {
        final var references = new HashSet<Reference>();
        userDao.findAll().stream()
                .filter(u -> StringUtils.isNotEmpty(u.getPhotoFilename()))
                .forEach(u -> references.add(new Reference("User", u.getId(), u.getPhotoFilename())));

        final var questions = questionDao.findAll();
        questions.stream()
                .filter(q -> StringUtils.isNotEmpty(q.getPhotoFilename()))
                .forEach(q -> references.add(new Reference("Question", q.getId(), q.getPhotoFilename())));

        questions.stream()
                .flatMap(question -> question.getAnswers().stream())
                .filter(a -> StringUtils.isNotEmpty(a.getPhotoFilename()))
                .forEach(a -> references.add(new Reference("Answer", a.getId(), a.getPhotoFilename())));

        questionHintDao.findAll().stream()
                .filter(h -> StringUtils.isNotEmpty(h.getPhotoFilename()))
                .forEach(h -> references.add(new Reference("Hint", h.getId(), h.getPhotoFilename())));

        return references;
    }

    record Reference(String entity, UUID id, String photoFilename) {
    }
}
