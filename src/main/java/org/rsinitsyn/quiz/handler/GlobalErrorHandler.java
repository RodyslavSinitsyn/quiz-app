package org.rsinitsyn.quiz.handler;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.server.DefaultErrorHandler;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GlobalErrorHandler extends DefaultErrorHandler {

    @Override
    public void error(ErrorEvent event) {
        var errMessage = event.getThrowable().getMessage();
        log.error("Server error.", event.getThrowable());
        // TODO Notification is short (< sec)
//        Notification notification = Notification.show(errMessage, 3_000, Notification.Position.TOP_END);
//        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//        notification.addClassNames(LumoUtility.FontSize.MEDIUM);

        // TODO Temp
        Optional.ofNullable(UI.getCurrent())
                .ifPresent(ui -> {
                    ui.access(() -> {
                        Button cancelButton = new Button();
                        cancelButton.addClickShortcut(Key.ESCAPE);
                        cancelButton.setText("Выйти");

                        ConfirmDialog confirmDialog = new ConfirmDialog();
                        confirmDialog.setConfirmButton(cancelButton);
                        confirmDialog.setHeader("Произошла ошибка");
                        confirmDialog.setText(errMessage);
                        confirmDialog.addClassNames(LumoUtility.TextColor.ERROR_CONTRAST);
                        confirmDialog.open();
                    });
                });
    }
}
