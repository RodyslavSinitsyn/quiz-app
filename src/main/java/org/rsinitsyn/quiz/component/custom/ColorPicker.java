package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;

@Tag("input")
public class ColorPicker extends AbstractSinglePropertyField<ColorPicker, String> {
    public ColorPicker() {
        super("value", "#000000", false);
        getElement().setAttribute("type", "color");
        setSynchronizedEvent("change");
    }
}