package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SessionWrapper {

    public static String getLoggedUser() {
        var loggedUserEntity = getLoggedUserEntity();
        if (loggedUserEntity == null) {
            return "Аноним";
        }
        return loggedUserEntity.getUsername();
    }

    public static UserEntity getLoggedUserEntity() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getPrincipal() instanceof UserEntity)
                .map(Authentication::getPrincipal)
                .map(UserEntity.class::cast)
                .orElse(null);
    }

    public static boolean isAuthenticated() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map((auth) -> !(auth instanceof AnonymousAuthenticationToken))
                .orElse(false);
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
