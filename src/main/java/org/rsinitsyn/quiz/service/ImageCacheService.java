package org.rsinitsyn.quiz.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.rsinitsyn.quiz.utils.QuizUtils.readImageFile;

@Service
public class ImageCacheService {

    private final Cache<String, byte[]> cache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    public byte[] load(String filename) {
        try {
            return cache.get(filename, () -> readFromDisk(filename));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public void evict(String filename) {
        cache.invalidate(filename);
    }

    private byte[] readFromDisk(String filename) throws IOException {
        return Files.readAllBytes(readImageFile(filename).toPath());
    }
}
