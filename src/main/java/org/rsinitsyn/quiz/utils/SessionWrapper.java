package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.server.VaadinSession;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SessionWrapper {

    public static final String ADMIN_NAME = "admin";

    public static String getLoggedUser() {
        return getLoggedUserEntity()
                .map(UserEntity::getUsername)
                .orElse("Аноним");
    }

    public static String getLoggedUserPhoto() {
        return getLoggedUserEntity()
                .map(UserEntity::getPhotoFilename)
                .orElse(StringUtils.EMPTY);
    }

    public static String getLoggedUserThemeColor() {
        return Optional.ofNullable(VaadinSession.getCurrent())
                .map(s -> s.getAttribute(ThemeUtils.THEME_COLOR_KEY))
                .map(String::valueOf)
                .orElseGet(() -> ThemeUtils.BLACK_COLOR);
    }

    public static Optional<UserEntity> getLoggedUserEntity() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(authentication -> authentication.getPrincipal() instanceof UserEntity)
                .map(Authentication::getPrincipal)
                .map(UserEntity.class::cast);
    }

    public static boolean isAuthenticated() {
        return Optional.of(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .map((auth) -> !(auth instanceof AnonymousAuthenticationToken))
                .orElse(false);
    }
}
