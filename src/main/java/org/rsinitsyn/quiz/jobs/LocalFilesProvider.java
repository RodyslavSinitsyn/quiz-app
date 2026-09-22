package org.rsinitsyn.quiz.jobs;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Component
public class LocalFilesProvider implements FilesProvider {

    @Override
    public Set<Path> getFiles(final Path path) {
        try (final var files = Files.list(path)) {
            return files.collect(toSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
