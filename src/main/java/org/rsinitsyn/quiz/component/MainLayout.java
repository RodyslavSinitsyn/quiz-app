package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.page.GameTypePage;
import org.rsinitsyn.quiz.page.QuestionsListPage;
import org.rsinitsyn.quiz.page.StatisticPage;
import org.rsinitsyn.quiz.utils.QuizResourceUtils;

public class MainLayout extends AppLayout {

    private FlexLayout header = new FlexLayout();

    private H1 logo = new H1();
    private Tabs tabs = new Tabs();
    private HorizontalLayout authLayout = new HorizontalLayout();

    private Button loginButton = new Button();
    private Span loggedUserNameSpan = new Span();
    private Button exitButton = new Button();

    private Dialog dialog = new Dialog();

    public MainLayout() {
        configureLogo();
        configureTabs();
        configureDialog();
        configureAuth();

        authLayout.add(loggedUserNameSpan, loginButton, exitButton);
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
        tabs.add(createTab("Играть", GameTypePage.class));
        tabs.add(createTab("Вопросы", QuestionsListPage.class));
        tabs.add(createTab("Статистика", StatisticPage.class));
    }

    private Tab createTab(String text, Class<? extends Component> navigateTo) {
        Tab tab = new Tab(text);
        RouterLink link = new RouterLink(navigateTo);
        tab.add(link); // TODO Fix style
        return tab;
    }

    private void configureAuth() {
        loginButton.setText("Войти");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickListener(event -> {
            dialog.open();
        });

        exitButton.setText("Выйти");
        exitButton.addClickListener(event -> {
            renderAfterLogout();
            VaadinSession.getCurrent().setAttribute("user", null);
        });

        if (QuizResourceUtils.getLoggedUser().equals("Аноним")) {
            renderAfterLogout();
        } else {
            renderAfterLogin(QuizResourceUtils.getLoggedUser());
        }
    }

    private void configureDialog() {
        dialog.setHeaderTitle("Введите имя пользователя");

        TextField userNameInput = new TextField();
        userNameInput.setWidthFull();

        Button submit = new Button();
        submit.setText("Войти");
        submit.setWidthFull();
        submit.addClickShortcut(Key.ENTER);
        submit.addClickListener(event -> {
            VaadinSession.getCurrent().setAttribute("user", userNameInput.getValue());
            renderAfterLogin(userNameInput.getValue());
            dialog.close();
        });

        dialog.add(userNameInput, submit);
    }

    private void renderAfterLogin(String loggedUserName) {
        loggedUserNameSpan.setVisible(true);
        loggedUserNameSpan.setText(loggedUserName);
        loginButton.setVisible(false);
        exitButton.setVisible(true);

    }

    private void renderAfterLogout() {
        loggedUserNameSpan.setVisible(false);
        loggedUserNameSpan.setText("");
        loginButton.setVisible(true);
        exitButton.setVisible(false);
    }
}
