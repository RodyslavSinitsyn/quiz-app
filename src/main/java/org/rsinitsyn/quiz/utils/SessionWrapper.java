package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.VaadinSession;
import org.apache.commons.lang3.StringUtils;

public class SessionWrapper {

    public static String getLoggedUser() {
        return StringUtils.defaultIfEmpty(
                (String) VaadinSession.getCurrent().getAttribute("user"),
                "Аноним"
        );
    }

    public static void setTheme(String theme) {
        VaadinSession.getCurrent().setAttribute("theme", theme);
    }

    public static String getTheme() {
        return (String) VaadinSession.getCurrent().getAttribute("theme");
    }
}
