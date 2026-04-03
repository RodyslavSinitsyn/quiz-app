package org.rsinitsyn.quiz.jobs;

import java.nio.file.Path;
import java.util.Set;

public interface FilesProvider {
    Set<Path> getFiles(Path path);
}
