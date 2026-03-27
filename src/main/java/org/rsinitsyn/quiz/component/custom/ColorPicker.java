package org.rsinitsyn.quiz.component.custom;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.Tag;
import org.rsinitsyn.quiz.utils.ThemeUtils;

import static org.rsinitsyn.quiz.utils.ThemeUtils.BLACK_COLOR;

@Tag("input")
public class ColorPicker extends AbstractSinglePropertyField<ColorPicker, String> {
    public ColorPicker() {
        super("value", BLACK_COLOR, false);
        getElement().setAttribute("type", "color");
        setSynchronizedEvent("change");
    }
}