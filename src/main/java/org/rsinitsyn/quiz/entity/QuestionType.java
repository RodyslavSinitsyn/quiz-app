package org.rsinitsyn.quiz.entity;

import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum QuestionType {
    TEXT(VaadinIcon.QUESTION.create()),
    PHOTO(VaadinIcon.PICTURE.create()),
    MULTI(VaadinIcon.QUESTION.create()),
    PRECISION((VaadinIcon.DOT_CIRCLE.create())),
    OR(VaadinIcon.CORNER_UPPER_LEFT.create()),
    TOP(VaadinIcon.LIST_UL.create()),
    LINK(VaadinIcon.LINK.create()),
    SEQUENCE(VaadinIcon.LIST_OL.create());

    public final Icon icon;
}
