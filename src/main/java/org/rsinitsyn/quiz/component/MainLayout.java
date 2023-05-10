package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Arrays;
import org.rsinitsyn.quiz.entity.UserEntity;
import org.rsinitsyn.quiz.page.FontsPage;
import org.rsinitsyn.quiz.page.NewGamePage;
import org.rsinitsyn.quiz.page.QuestionsListPage;
import org.rsinitsyn.quiz.page.StatisticPage;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.SessionWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class MainLayout extends AppLayout {

    private HorizontalLayout header = new HorizontalLayout();

    private Button loginButton = new Button();
    private Span warningMessageAboutLogin = new Span();
    private Span loggedUserNameSpan = new Span();
    private Button exitButton = new Button();

    private Dialog dialog = new Dialog();

    private UserService userService;
    private Environment environment;

    @Autowired
    public MainLayout(UserService userService,
                      Environment environment) {
        this.userService = userService;
        this.environment = environment;

        configureDialog();
        configureAuthComponents();

        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.add(
                createLogo(),
                createTabs(),
                createAuthLayout());
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.PRIMARY);

        addToNavbar(true, header);
        addClassNames(LumoUtility.Background.PRIMARY_10);
    }

    private Span createLogo() {
        Span logo = new Span();
        logo.addClassNames(LumoUtility.Margin.MEDIUM, LumoUtility.FontSize.XLARGE);
        logo.add(VaadinIcon.ACADEMY_CAP.create());
        return logo;
    }

    private Tabs createTabs() {
        Tabs tabs = new Tabs();
        tabs.addThemeVariants(TabsVariant.LUMO_CENTERED);
        tabs.add(createTab("Играть", VaadinIcon.PLAY_CIRCLE_O.create(), NewGamePage.class));
        tabs.add(createTab("Вопросы", VaadinIcon.QUESTION_CIRCLE_O.create(), QuestionsListPage.class));
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
        authLayout.add(loggedUserNameSpan, warningMessageAboutLogin, loginButton, exitButton);
        authLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        return authLayout;
    }

    private void configureAuthComponents() {
        warningMessageAboutLogin.setText("НЕ рекомендуется использовать Анонимный мод");
        warningMessageAboutLogin.addClassNames(LumoUtility.TextColor.ERROR);

        loginButton.setText("Войти");
        loginButton.setIcon(VaadinIcon.SIGN_IN.create());
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickListener(event -> {
            dialog.open();
        });

        exitButton.setText("Выйти");
        exitButton.setIcon(VaadinIcon.SIGN_OUT.create());
        exitButton.addClickListener(event -> {
            renderAfterLogout();
            VaadinSession.getCurrent().close();
        });

        if (SessionWrapper.getLoggedUser().equals("Аноним")) {
            renderAfterLogout();
            if (!dialog.isOpened()) {
                dialog.open();
            }
        } else {
            renderAfterLogin(SessionWrapper.getLoggedUser());
        }
    }

    private void configureDialog() {
        dialog.setCloseOnOutsideClick(false);
        dialog.setCloseOnEsc(true);
        dialog.setHeaderTitle("Войдите");

        TextField userNameInput = new TextField();
        userNameInput.setWidthFull();
        userNameInput.setLabel("Имя нового пользователя");
        userNameInput.setRequired(true);
        userNameInput.setTooltipText("Ввод нового имени автоматически создаст нового пользователя");

        Button submit = new Button();
        submit.setText("Войти");
        submit.setWidthFull();
        submit.addClickShortcut(Key.ENTER);
        submit.addClickListener(event -> loginUser(userNameInput.getValue()));

        Select<String> users = new Select<>();
        users.setLabel("Выбрать из уже созданных");
        users.setWidthFull();
        users.setItems(userService.findAllOrderByVisitDateDesc().stream().map(UserEntity::getUsername).toList());
        users.addValueChangeListener(event -> loginUser(event.getValue()));

        dialog.add(users, userNameInput, submit);
    }

    private void loginUser(String username) {
        userService.loginUser(username);
        VaadinSession.getCurrent().setAttribute("user", username);
        renderAfterLogin(username);
        dialog.close();
        getUI().ifPresent(ui -> ui.getPage().reload());
    }

    private void renderAfterLogin(String loggedUserName) {
        loggedUserNameSpan.setVisible(true);
        loggedUserNameSpan.setText(loggedUserName);
        loggedUserNameSpan.addClassNames(LumoUtility.FontWeight.BOLD);
        loginButton.setVisible(false);
        warningMessageAboutLogin.setVisible(false);
        exitButton.setVisible(true);
    }

    private void renderAfterLogout() {
        loggedUserNameSpan.setVisible(false);
        loggedUserNameSpan.setText("");
        loginButton.setVisible(true);
        warningMessageAboutLogin.setVisible(true);
        exitButton.setVisible(false);
    }
}
