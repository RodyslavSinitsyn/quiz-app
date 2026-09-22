package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.utils.SessionWrapper;

@Route(value = "/login", layout = MainLayout.class)
@PageTitle("Login")
@AnonymousAllowed
public class LoginPage extends VerticalLayout implements BeforeEnterObserver, HasUrlParameter<String> {

    // https://vaadin.com/docs/latest/components/login
    private final LoginForm loginForm = new LoginForm();
    private String registeredUsername;

    public LoginPage() {
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSizeFull();

        addClassName("login-rich-content");

        loginForm.setAction("login");
        loginForm.setForgotPasswordButtonVisible(false);

        Anchor registerLink = new Anchor("/registration", "Doesn't have an account? Sign Up");
        registerLink.addClassName("register-link");

        add(loginForm, registerLink);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (SessionWrapper.isAuthenticated()) {
            beforeEnterEvent.rerouteTo("/");
            return;
        }
        if (StringUtils.isNotEmpty(registeredUsername)) {
            loginForm.getElement().executeJs("this.$.vaadinLoginUsername.value = $0;", registeredUsername);
        }
        if (beforeEnterEvent.getLocation()
                .getQueryParameters()
                .getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter String registeredUsername) {
        this.registeredUsername = registeredUsername;
    }
}