package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.page.GameTypePage;
import org.rsinitsyn.quiz.page.QuestionsListPage;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        H1 logo = new H1("Who Knows!?");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.TextAlignment.LEFT,
                LumoUtility.Margin.SMALL);
        logo.add(new RouterLink(GameTypePage.class));

        addToNavbar(logo, createTabs());
    }

    private Tabs createTabs() {
        Tab playTab = new Tab("Играть");
        playTab.add(new RouterLink(GameTypePage.class));

        Tab listTab = new Tab("Вопросы");
        listTab.add(new RouterLink(QuestionsListPage.class));

        Tab aboutUsTab = new Tab("О нас");

        Tabs tabs = new Tabs(playTab, listTab, aboutUsTab);
        return tabs;
    }
}
