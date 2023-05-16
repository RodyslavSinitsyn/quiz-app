package org.rsinitsyn.quiz.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Push
@Theme(value = "quiz-theme", variant = Lumo.LIGHT)
public class ApplicationConfiguration implements AppShellConfigurator, VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent event) {
//        event.getSource().setSystemMessagesProvider((SystemMessagesProvider)
//                systemMessagesInfo -> {
//                    var messages = new CustomizedSystemMessages();
//                    messages.setSessionExpiredCaption("Выход из системы");
//                    messages.setSessionExpiredMessage("Из за бездействия ваша сессия была закрыта.");
//                    messages.setSessionExpiredNotificationEnabled(true);
//                    return messages;
//                });
        event.getSource().addSessionInitListener(
                e -> {
                    log.info("A new Session has been initialized: {}", e.getSession().getSession().getId());
                    e.getSession().setErrorHandler(new GlobalErrorHandler());
                    e.getSession().getSession().setMaxInactiveInterval(3_600); // one hour
                });
        event.getSource().addSessionDestroyListener(e -> {
            log.info("Session closed: {}", e.getSession().getSession().getId());
        });
    }
}
