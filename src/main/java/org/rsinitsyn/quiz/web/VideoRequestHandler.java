package org.rsinitsyn.quiz.web;

import com.vaadin.flow.server.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;

import static org.rsinitsyn.quiz.utils.QuizUtils.readVideoFile;

@Component
@RequiredArgsConstructor
public class VideoRequestHandler implements RequestHandler, VaadinServiceInitListener {

    public static final String PATH = "/quiz-videos/";

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
        final var data = Files.readAllBytes(readVideoFile(filename).toPath());

        response.setContentType("mp4");
        response.getOutputStream().write(data);
        return true;
    }
}
