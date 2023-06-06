package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import java.util.function.BiConsumer;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;

@UtilityClass
public class QuizComponents {

    public H1 mainHeader(String text) {
        var header = new H1(text);
        header.addClassNames(
                LumoUtility.AlignSelf.CENTER,
                LumoUtility.TextAlignment.CENTER);
        return header;
    }

    public H4 subHeader(String text) {
        return new H4(text);
    }

    public ConfirmDialog openConfirmDialog(Component content,
                                           String headerText,
                                           Runnable confirmAction) {
        var dialog = new ConfirmDialog();
        dialog.setRejectable(false);
        dialog.setCloseOnEsc(true);
        dialog.setCancelable(true);
        dialog.setHeader(StringUtils.defaultIfEmpty(headerText, ""));
        dialog.setText(content);
        dialog.setCancelText("Отменить");
        dialog.setConfirmText("Подтвердить");
        dialog.addCancelListener(event -> dialog.close());
        dialog.addConfirmListener(event -> {
            confirmAction.run();
            dialog.close();
        });
        dialog.open();
        return dialog;
    }

    public Notification infoNotification(String text) {
        return Notification.show(text,
                2_000,
                Notification.Position.TOP_STRETCH);
    }

    public <T extends Component> T appendTextBorder(T component) {
        component.getStyle().set("text-shadow", StaticValuesHolder.getFontBorder());
//        component.getStyle().set("-webkit-text-stroke-width", "1px");
//        component.getStyle().set("-webkit-text-stroke-color", "black");
        return component;
    }

    public Span questionLinkedWithGameIcon(QuestionEntity question) {
        if (question.presentInAnyGame()) {
            Icon icon = VaadinIcon.LINK.create();
            icon.setTooltipText("Вопрос связан с игрой и не может быть удален");
            return new Span(icon);
        }
        return new Span();
    }

    public Span questionMechanicSpan(QuestionEntity question) {
        return questionMechanicSpan(question.isOptionsOnly(), question.getType());
    }

    public Span questionMechanicSpan(boolean optionsOnly, QuestionType questionType) {
        Span result = new Span();
        if (optionsOnly) {
            result.getElement().getThemeList().add("badge contrast");
        } else if (questionType.equals(QuestionType.TOP)) {
            result.getElement().getThemeList().add("badge error");
        } else {
            result.getElement().getThemeList().add("badge");
        }
        Icon typeIcon;
        switch (questionType) {
            case TOP -> typeIcon = VaadinIcon.LIST_OL.create();
            case TEXT -> typeIcon = VaadinIcon.QUESTION.create();
            case OR -> typeIcon = VaadinIcon.CORNER_UPPER_LEFT.create();
            case MULTI -> typeIcon = VaadinIcon.LIST_UL.create();
            case PHOTO -> typeIcon = VaadinIcon.PICTURE.create();
            default -> typeIcon = VaadinIcon.DOT_CIRCLE.create();
        }
        result.add(typeIcon);
        return result;
    }

    public Upload uploadComponent(String uploadLabel, BiConsumer<MemoryBuffer, SucceededEvent> eventHandler, String allowedTypes) {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setUploadButton(new Button(uploadLabel));
        upload.setDropAllowed(true);
        upload.setDropLabel(new Label(""));
        upload.setAcceptedFileTypes(allowedTypes);
        upload.addSucceededListener(event -> eventHandler.accept(buffer, event));
        return upload;
    }

    public HorizontalLayout questionDescription(QuestionEntity question) {
        HorizontalLayout row = new HorizontalLayout();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        if (StringUtils.isNotEmpty(question.getAudioFilename())) {
            Icon playSound = VaadinIcon.PLAY_CIRCLE.create();
            playSound.addClickListener(event -> AudioUtils.playSoundAsync(question.getAudioFilename()));
            row.add(playSound);
        }
        if (StringUtils.isNotEmpty(question.getPhotoFilename())) {
            row.add(avatar(question.getPhotoFilename()));
        }
        row.add(new Span(
                question.getText().length() > 300
                        ? question.getText().substring(0, 300).concat("...")
                        : question.getText()));
        return row;
    }

    public Avatar avatar(String photoFilename) {
        Avatar avatar = new Avatar();
        avatar.setImageResource(
                QuizUtils.createStreamResourceForPhoto(photoFilename));
        return avatar;
    }
}
