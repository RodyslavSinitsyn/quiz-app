package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.rsinitsyn.quiz.component.MainLayout;
import org.rsinitsyn.quiz.service.UserService;
import org.rsinitsyn.quiz.utils.SessionWrapper;

// TODO: Add header, fix width, add white border same as for login
@Route(value = "/registration", layout = MainLayout.class)
@PageTitle("Registration")
@AnonymousAllowed
public class RegistrationPage extends VerticalLayout implements BeforeEnterObserver {

    private final UserService userService;

    public RegistrationPage(UserService userService) {
        this.userService = userService;

        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        setSizeFull();

        TextField userNameInput = new TextField();
        userNameInput.setWidthFull();
        userNameInput.setLabel("Имя пользователя");
        userNameInput.setRequired(true);

        PasswordField passwordInput = new PasswordField();
        passwordInput.setWidthFull();
        passwordInput.setLabel("Пароль");
        passwordInput.setRequired(true);

        Button submit = new Button();
        submit.setText("Зарегестрироваться");
        submit.setWidthFull();
        submit.addClickShortcut(Key.ENTER);
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.addClickListener(registerUserHandler(userService, userNameInput, passwordInput));

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        formLayout.add(userNameInput, passwordInput, submit);


        Anchor loginLink = new Anchor("/login", "Already have an account? Sign In");

        add(formLayout, loginLink);
    }

    private static ComponentEventListener<ClickEvent<Button>> registerUserHandler(UserService userService,
                                                                                  TextField userNameInput,
                                                                                  PasswordField passwordInput) {
        return event -> {
            var registeredUser = userService.registerUser(userNameInput.getValue(), passwordInput.getValue());
            event.getSource().getUI()
                    .ifPresent(ui -> ui.navigate(LoginPage.class, registeredUser.getUsername()));
        };
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (SessionWrapper.isAuthenticated()) {
            beforeEnterEvent.rerouteTo("/");
        }
    }
}
