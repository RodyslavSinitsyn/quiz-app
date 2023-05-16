package org.rsinitsyn.quiz.config;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.utils.QuizComponents;

@Slf4j
public class GlobalErrorHandler extends DefaultErrorHandler {

    @Override
    public void error(ErrorEvent event) {
        var errMessage = event.getThrowable().getMessage();
        log.error("Server error.", event.getThrowable());
        Optional.ofNullable(UI.getCurrent())
                .ifPresent(ui -> {
                    ui.access(() -> {
                        QuizComponents.openConfirmDialog(
                                new Span(errMessage),
                                "Произошла ошибка",
                                () -> {
                                }
                        );
                    });
                });
    }
}
