package org.rsinitsyn.quiz.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.page.GameTypePage;
import org.rsinitsyn.quiz.page.QuestionsListPage;
import org.rsinitsyn.quiz.page.StatisticPage;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        H1 logo = new H1("Who Knows!?");
        logo.addClassNames(
                LumoUtility.FontSize.LARGE,
                LumoUtility.TextAlignment.LEFT,
                LumoUtility.Margin.MEDIUM);

        addToNavbar(logo, createTabs());
    }

    private Tabs createTabs() {
        Tabs tabs = new Tabs();
        tabs.add(createTab("Играть", GameTypePage.class));
        tabs.add(createTab("Вопросы", QuestionsListPage.class));
        tabs.add(createTab("Статистика", StatisticPage.class));
        return tabs;
    }

    private Tab createTab(String text, Class<? extends Component> navigateTo) {
        Tab tab = new Tab(text);
        RouterLink link = new RouterLink(navigateTo);
        tab.add(link); // TODO Fix style
        return tab;
    }
}
