package org.rsinitsyn.quiz.web;

import com.vaadin.flow.server.*;
import lombok.RequiredArgsConstructor;
import org.rsinitsyn.quiz.service.ImageCacheService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ImageRequestHandler implements RequestHandler, VaadinServiceInitListener {

    public static final String PATH = "/quiz-images/";
    public static final int CACHE_TIME = 3600;

    private final ImageCacheService imageCache;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addRequestHandler(this);
    }

    @Override
    public boolean handleRequest(VaadinSession session, VaadinRequest request, VaadinResponse response) throws IOException {
        final var path = request.getPathInfo();
        if (path == null || !path.startsWith(PATH)) {
            return false;
        }

        final var filename = path.substring(PATH.length());
        final var data = imageCache.load(filename);

        response.setContentType(resolveContentType(filename));
        response.setCacheTime(CACHE_TIME);
        response.getOutputStream().write(data);
        return true;
    }

    private String resolveContentType(String filename) {
        return filename.endsWith(".png") ? "image/png" : "image/jpeg";
    }
}
