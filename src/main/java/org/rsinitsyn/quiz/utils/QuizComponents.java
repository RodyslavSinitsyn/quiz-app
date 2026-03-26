package org.rsinitsyn.quiz.utils;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.apache.commons.lang3.StringUtils;
import org.rsinitsyn.quiz.entity.QuestionEntity;
import org.rsinitsyn.quiz.entity.QuestionType;

import java.io.InputStream;
import java.util.function.BiConsumer;

import static org.rsinitsyn.quiz.component.cleverest_old.CleverestComponents.horizontalLayoutBetween;
import static org.rsinitsyn.quiz.utils.QuizUtils.createStreamResourceForPhoto;

public final class QuizComponents {

    private QuizComponents() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static H1 mainHeader(String text) {
        var header = new H1(text);
        header.addClassNames(
                LumoUtility.AlignSelf.CENTER,
                LumoUtility.TextAlignment.CENTER);
        return header;
    }

    public static H4 subHeader(String text) {
        return new H4(text);
    }

    public static ConfirmDialog openConfirmDialog(Component content,
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

    public static Notification infoNotification(String text) {
        return Notification.show(text,
                2_000,
                Notification.Position.TOP_STRETCH);
    }

    public static <T extends Component> T appendTextBorder(T component) {
        component.getStyle().set("text-shadow", StaticValuesHolder.getFontBorder());
//        component.getStyle().set("-webkit-text-stroke-width", "1px");
//        component.getStyle().set("-webkit-text-stroke-color", "black");
        return component;
    }

    public static Span questionLinkedWithGameIcon(QuestionEntity question) {
        if (question.presentInAnyGame()) {
            Icon icon = VaadinIcon.LINK.create();
            icon.setTooltipText("Вопрос связан с игрой и не может быть удален");
            return new Span(icon);
        }
        return new Span();
    }

    public static Span questionMechanicSpan(QuestionEntity question) {
        return questionMechanicSpan(question.isOptionsOnly(), question.getType());
    }

    public static Span questionMechanicSpan(boolean optionsOnly, QuestionType questionType) {
        Span result = new Span();
        if (optionsOnly) {
            result.getElement().getThemeList().add("badge contrast");
        } else {
            result.getElement().getThemeList().add("badge");
        }
        result.add(questionType.icon.create());
        return result;
    }

    public static Upload uploadComponent(String uploadLabel,
                                         BiConsumer<MultiFileMemoryBuffer, SucceededEvent> eventHandler,
                                         String allowedTypes,
                                         int maxFiles) {
        final var buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setUploadButton(new Button(uploadLabel));
        upload.setDropAllowed(true);
        upload.setMaxFiles(maxFiles);
        upload.setMaxFileSize(1024 * 1024 * 50);
        upload.setDropLabel(new NativeLabel("Выбрать файлы"));
        if (allowedTypes != null) {
            upload.setAcceptedFileTypes(allowedTypes);
        }
        upload.addSucceededListener(event -> eventHandler.accept(buffer, event));
        return upload;
    }

    public static HorizontalLayout questionDescription(QuestionEntity question) {
        final var row = horizontalLayoutBetween();
        if (StringUtils.isNotEmpty(question.getAudioFilename())) {
            Icon playSound = VaadinIcon.PLAY_CIRCLE.create();
            playSound.addClickListener(event -> AudioUtils.playSoundAsync(question.getAudioFilename()));
            row.add(playSound);
        }
        if (StringUtils.isNotEmpty(question.getPhotoFilename())) {
            row.add(smallAvatar(question.getPhotoFilename()));
        }
        row.add(new Span(question.getTextTruncated(300)));
        return row;
    }

    public static Avatar largeAvatar(String photoFilename) {
        return avatar(photoFilename, AvatarVariant.LUMO_XLARGE);
    }

    public static Avatar smallAvatar(String photoFilename) {
        return avatar(photoFilename);
    }

    public static Avatar avatar(String photoFilename, AvatarVariant... variant) {
        Avatar avatar = new Avatar();
        avatar.addThemeVariants(variant);
        avatar.setImageResource(createStreamResourceForPhoto(photoFilename));
        return avatar;
    }

    public static Avatar avatar(InputStream photoData, AvatarVariant... variant) {
        Avatar avatar = new Avatar();
        avatar.addThemeVariants(variant);
        avatar.setImageResource(createStreamResourceForPhoto("name", photoData));
        return avatar;
    }
}
