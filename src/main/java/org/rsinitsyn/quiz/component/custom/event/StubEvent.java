package org.rsinitsyn.quiz.component.custom.event;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.html.Div;

public class StubEvent extends ComponentEvent<Div> {
    public StubEvent() {
        super(new Div(), true);
    }
}
