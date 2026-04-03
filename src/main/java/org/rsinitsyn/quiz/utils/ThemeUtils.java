package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.theme.ThemePreset;

import java.awt.*;
import java.util.List;

import static com.vaadin.flow.component.page.WebStorage.Storage.LOCAL_STORAGE;
import static java.util.Optional.ofNullable;

public class ThemeUtils {

    public static final String BLACK_COLOR = "#000000";

    public static final List<ThemePreset> THEME_PRESETS = List.of(
            new ThemePreset("Ocean", "#1976d2"),
            new ThemePreset("Emerald", "#2e7d32"),
            new ThemePreset("Sunset", "#d84315"),
            new ThemePreset("Purple", "#6a1b9a"),
            new ThemePreset("Brown", "#6d4c41"),
            new ThemePreset("Pink", "#d81b60"),
            new ThemePreset("Graphite", "#263238"),
            new ThemePreset("Deep Blue", "#0d47a1")
    );
    public static final String THEME_COLOR_KEY = "quiz-theme-color";

    public static void applyTheme(UI ui, String color) {
        if (StringUtils.isEmpty(color)) {
            return;
        }
        final var rgb = hexToRgb(color);
        ui.getElement().executeJs("""
                            document.documentElement.style.setProperty('--lumo-primary-color', $0);
                            document.documentElement.style.setProperty('--lumo-primary-color-50pct', $1);
                            document.documentElement.style.setProperty('--lumo-primary-color-10pct', $2);
                            document.documentElement.style.setProperty('--lumo-primary-text-color', $0);
                        """,
                color,
                "rgba(%d,%d,%d,0.5)".formatted(rgb[0], rgb[1], rgb[2]),
                "rgba(%d,%d,%d,0.1)".formatted(rgb[0], rgb[1], rgb[2])
        );
        VaadinSession.getCurrent().setAttribute(THEME_COLOR_KEY, color);
        WebStorage.setItem(ui, LOCAL_STORAGE, THEME_COLOR_KEY, color);
    }

    private static int[] hexToRgb(String hex) {
        final var color = Color.decode(hex);
        return new int[]{
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        };
    }

    public static void updateTheme(UI ui) {
        var js = "document.documentElement.setAttribute('theme', $0)";
        ui.getElement().executeJs(js, ThemeUtils.getThemeMode());

        ofNullable(VaadinSession.getCurrent().getAttribute(THEME_COLOR_KEY))
                .map(String.class::cast)
                .ifPresentOrElse(val -> applyTheme(ui, val),
                        () -> WebStorage.getItem(ui, LOCAL_STORAGE, THEME_COLOR_KEY)
                                .thenAccept(val -> applyTheme(ui, val)));
    }

    public static void setThemeMode(String theme) {
        VaadinSession.getCurrent().setAttribute("theme", theme);
    }

    public static String getThemeMode() {
        return StringUtils.defaultIfEmpty(
                (String) VaadinSession.getCurrent().getAttribute("theme"),
                Lumo.LIGHT
        );
    }
}
