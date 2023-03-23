package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.rsinitsyn.quiz.component.MainLayout;

@Route(value = "/statistic", layout = MainLayout.class)
@PageTitle("Statistic")
public class StatisticPage extends VerticalLayout {

    public StatisticPage() {
        add(new Span("Stats will be here soon..."));
    }
}
