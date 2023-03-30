package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.page.NewGamePage;
import org.rsinitsyn.quiz.page.QuestionsListPage;
import org.rsinitsyn.quiz.page.StatisticPage;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.QuizUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class MainLayout extends AppLayout {

    private FlexLayout header = new FlexLayout();

    private H1 logo = new H1();
    private Tabs tabs = new Tabs();
    private HorizontalLayout authLayout = new HorizontalLayout();

    private Button loginButton = new Button();
    private Span warningMessageAboutLogin = new Span();
    private Span loggedUserNameSpan = new Span();
    private Button exitButton = new Button();

    private Dialog dialog = new Dialog();

    private UserService userService;

    @Autowired
    public MainLayout(UserService userService) {
        this.userService = userService;
        configureLogo();
        configureTabs();
        configureDialog();
        configureAuth();

        authLayout.add(loggedUserNameSpan, loginButton, warningMessageAboutLogin, exitButton);
        authLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        header.setWidthFull();
        header.add(logo, tabs, authLayout);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setAlignContent(FlexLayout.ContentAlignment.STRETCH);
        header.setFlexDirection(FlexLayout.FlexDirection.ROW);

        addToNavbar(header);
    }

    private void configureLogo() {
        logo.setText("ХЗ!?");
        logo.addClassNames(LumoUtility.Margin.MEDIUM);
    }

    private void configureTabs() {
        tabs.add(createTab("Играть", NewGamePage.class));
        tabs.add(createTab("Вопросы", QuestionsListPage.class));
        tabs.add(createTab("Статистика", StatisticPage.class));
    }

    private Tab createTab(String text, Class<? extends Component> navigateTo) {
        Tab tab = new Tab();
        RouterLink link = new RouterLink(navigateTo);
        link.setText(text);
        tab.add(link);
        return tab;
    }

    private void configureAuth() {
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

        if (QuizUtils.getLoggedUser().equals("Аноним")) {
            renderAfterLogout();
        } else {
            renderAfterLogin(QuizUtils.getLoggedUser());
        }
    }

    private void configureDialog() {
        dialog.setHeaderTitle("Войдите");

        TextField userNameInput = new TextField();
        userNameInput.setWidthFull();
        userNameInput.setLabel("Имя нового пользователя");
        userNameInput.setTooltipText("Ввод нового имени автоматически создаст нового пользователя");

        Button submit = new Button();
        submit.setText("Войти");
        submit.setWidthFull();
        submit.addClickShortcut(Key.ENTER);
        submit.addClickListener(event -> loginUser(userNameInput.getValue()));

        HorizontalLayout availableUsers = new HorizontalLayout();
        availableUsers.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
        userService.findAllOrderByVisitDateDesc().forEach(userEntity -> {
            Button button = new Button(userEntity.getUsername());
            button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.addClickListener(event -> loginUser(userEntity.getUsername()));
            availableUsers.add(button);
        });

        dialog.add(availableUsers, userNameInput, submit);
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
