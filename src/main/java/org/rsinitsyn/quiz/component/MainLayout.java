package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.*;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.page.*;
import org.rsinitsyn.quiz.utils.QuizComponents;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Slf4j
public class MainLayout extends AppLayout implements
        AfterNavigationObserver,
        BeforeEnterObserver,
        BeforeLeaveObserver {

    private HorizontalLayout header = new HorizontalLayout();

    private Button loginButton = new Button();
    private Span warningMessageAboutLogin = new Span();
    private Span loggedUserNameSpan = new Span();
    private Button exitButton = new Button();

    private Button themeToggle = new Button();
    private boolean darkTheme = false;

    private final Environment environment;
    private final AuthenticationContext authenticationContext;

    public MainLayout(Environment environment,
                      AuthenticationContext authenticationContext) {
        this.environment = environment;
        this.authenticationContext = authenticationContext;

        updateTheme();
        configureToggleTheme();
        configureAuthComponents();

        Tabs drawerTabs = createTabs(Tabs.Orientation.VERTICAL);
        drawerTabs.addClassName("navbar-drawer-tabs");
        Tabs navbarTabs = createTabs(Tabs.Orientation.HORIZONTAL);
        navbarTabs.addClassNames("navbar-tabs");

        DrawerToggle drawerToggle = new DrawerToggle();
        drawerToggle.addClassName("navbar-drawer-toggle");

        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.add(
                createLogoLayout(drawerToggle),
                navbarTabs,
                createAuthLayout());
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Border.BOTTOM,
                LumoUtility.BorderColor.PRIMARY);

        addToNavbar(header);
        addToDrawer(drawerTabs);
        setDrawerOpened(false);

        addClassNames(LumoUtility.Background.PRIMARY_10);
    }

    private void configureToggleTheme() {
        themeToggle.setIcon(VaadinIcon.MOON.create());
        themeToggle.addClickListener(event -> {
            darkTheme = !darkTheme;
            SessionWrapper.setTheme(darkTheme ? Lumo.DARK : Lumo.LIGHT);
            updateTheme();
        });
    }

    private HorizontalLayout createLogoLayout(DrawerToggle drawerToggle) {
        var logo = new HorizontalLayout();
        logo.setPadding(false);
        logo.setMargin(false);
        logo.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        logo.setAlignItems(FlexComponent.Alignment.CENTER);
        logo.addClassNames(LumoUtility.FontSize.LARGE,
                LumoUtility.Margin.Left.MEDIUM);
        logo.add(drawerToggle);
        logo.add(VaadinIcon.ACADEMY_CAP.create());
        return logo;
    }

    private Tabs createTabs(Tabs.Orientation orientation) {
        Tabs tabs = new Tabs();
        tabs.setOrientation(orientation);
        tabs.addThemeVariants(TabsVariant.LUMO_CENTERED,
                TabsVariant.LUMO_MINIMAL);
        tabs.add(createTab("Играть", VaadinIcon.PLAY_CIRCLE_O.create(), NewGamePage.class));
        tabs.add(createTab("Вопросы", VaadinIcon.QUESTION_CIRCLE_O.create(), QuestionsPage.class));
        tabs.add(createTab("Статистика", VaadinIcon.TRENDING_UP.create(), StatisticPage.class));
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            tabs.add(createTab("Шрифты", VaadinIcon.TEXT_LABEL.create(), FontsPage.class));
        }
        return tabs;
    }

    private Tab createTab(String text,
                          Icon icon,
                          Class<? extends Component> navigateTo) {
        Tab tab = new Tab();
        RouterLink link = new RouterLink(navigateTo);
        link.setText(text);
        tab.add(link);
        tab.addComponentAsFirst(icon);
        return tab;
    }

    private HorizontalLayout createAuthLayout() {
        HorizontalLayout authLayout = new HorizontalLayout();
        authLayout.add(themeToggle, loggedUserNameSpan, warningMessageAboutLogin, loginButton, exitButton);
        authLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        return authLayout;
    }

    private void configureAuthComponents() {
        warningMessageAboutLogin.setText("Аноним");
        warningMessageAboutLogin.addClassNames(LumoUtility.TextColor.ERROR);

        loginButton.setText("");
        loginButton.addClassNames(LumoUtility.Margin.MEDIUM);
        loginButton.setIcon(VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        loginButton.addClickListener(event ->
                getUI().ifPresent(ui -> ui.navigate(LoginPage.class)));

        exitButton.setText("");
        exitButton.setIcon(VaadinIcon.SIGN_OUT.create());
        exitButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        exitButton.addClassNames(LumoUtility.Margin.MEDIUM);
        exitButton.addClickListener(event -> {
            QuizComponents.openConfirmDialog(
                    new Span("Подтвердите действие"),
                    "Выйти из системы?",
                    () -> {
                        authenticationContext.logout();
                        renderAnonymous();
                    });
        });

        if(SessionWrapper.isAuthenticated()) {
            renderAuthorizedUser(SessionWrapper.getLoggedUser());
        } else {
            renderAnonymous();
        }
    }

    private void renderAuthorizedUser(String loggedUserName) {
        loggedUserNameSpan.setVisible(true);
        loggedUserNameSpan.setText(loggedUserName);
        loggedUserNameSpan.addClassNames(LumoUtility.FontWeight.BOLD);
        loginButton.setVisible(false);
        warningMessageAboutLogin.setVisible(false);
        exitButton.setVisible(true);
    }

    private void renderAnonymous() {
        loggedUserNameSpan.setVisible(false);
        loggedUserNameSpan.setText("");
        loginButton.setVisible(true);
        warningMessageAboutLogin.setVisible(true);
        exitButton.setVisible(false);
    }

    private void updateTheme() {
        var js = "document.documentElement.setAttribute('theme', $0)";
        getElement().executeJs(js, SessionWrapper.getTheme());
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        log.trace("afterNavigation");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        log.trace("beforeEnter");
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        log.trace("beforeLeave");
    }
}
