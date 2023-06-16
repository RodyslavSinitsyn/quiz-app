package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class SessionWrapper {

    public static String getLoggedUser() {
        return StringUtils.defaultIfEmpty(
                (String) Optional.ofNullable(VaadinSession.getCurrent())
                        .map(session -> session.getAttribute("user"))
                        .orElse(null),
                "Аноним"
        );
    }

    public static void setTheme(String theme) {
        VaadinSession.getCurrent().setAttribute("theme", theme);
    }

    public static String getTheme() {
        return StringUtils.defaultIfEmpty(
                (String) VaadinSession.getCurrent().getAttribute("theme"),
                Lumo.LIGHT
        );
    }
}
