package org.rsinitsyn.quiz.handler;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Push
public class ApplicationConfiguration implements AppShellConfigurator, VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addSessionInitListener(
                initEvent -> {
                    String host = initEvent.getRequest().getRemoteHost();
                    log.info("A new Session has been initialized: {}", host);
                    initEvent.getSession().setErrorHandler(new GlobalErrorHandler());
                });
    }
}
