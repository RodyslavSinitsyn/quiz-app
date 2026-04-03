package org.rsinitsyn.quiz.entity;

import com.vaadin.flow.component.icon.VaadinIcon;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum QuestionType {
    TEXT(VaadinIcon.QUESTION),
    PHOTO(VaadinIcon.PICTURE),
    MULTI(VaadinIcon.QUESTION),
    PRECISION((VaadinIcon.DOT_CIRCLE)),
    OR(VaadinIcon.CORNER_UPPER_LEFT),
    TOP(VaadinIcon.LIST_UL),
    LINK(VaadinIcon.LINK),
    SEQUENCE(VaadinIcon.LIST_OL),
    GUESS_PHOTO(VaadinIcon.EYE_SLASH);

    public final VaadinIcon icon;
}
