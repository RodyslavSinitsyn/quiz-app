package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import lombok.extern.slf4j.Slf4j;
import org.rsinitsyn.quiz.component.MainLayout;

@Route(value = "/fonts", layout = MainLayout.class)
@PageTitle("Fonts")
@Slf4j
public class FontsPage extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver, AfterNavigationObserver {

    public FontsPage() {
        log.info("constructor FontsPage");
        setSpacing(true);
        setPadding(true);
        setAlignItems(Alignment.START);
        setDefaultHorizontalComponentAlignment(Alignment.START);

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.THIN));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.THIN));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.EXTRALIGHT));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.EXTRALIGHT));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.LIGHT));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.LIGHT));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.NORMAL));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.NORMAL));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.MEDIUM));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.MEDIUM));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.SEMIBOLD));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.SEMIBOLD));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.BOLD));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.BOLD));

        add(new Span("-".repeat(100)));

        add(dummyText(LumoUtility.FontSize.XXSMALL, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.XSMALL, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.SMALL, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.MEDIUM, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.LARGE, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.XXLARGE, LumoUtility.FontWeight.EXTRABOLD));
        add(dummyText(LumoUtility.FontSize.XXXLARGE, LumoUtility.FontWeight.EXTRABOLD));

        add(new Span("constructor"));
    }

    private Span dummyText(String fontSize, String fontWeight) {
        Span span = new Span();
        span.setText(fontSize + " - " + fontWeight + ". Lorem Ipsum is simply dummy text of the printing");
        span.addClassNames(fontSize, fontWeight, LumoUtility.LineHeight.XSMALL);
        span.addComponentAsFirst(new Hr());
        return span;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent afterNavigationEvent) {
        log.info("afterNavigation: refresh {}, path {}",
                afterNavigationEvent.isRefreshEvent(),
                afterNavigationEvent.getLocation().getPath());
        add(new Span("afterNavigation"));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        log.info("beforeEnter: refresh {}",
                beforeEnterEvent.isRefreshEvent());
        add(new Span("beforeEnter"));
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent beforeLeaveEvent) {
        log.info("beforeLeave: continueaction {}, path: {}",
                beforeLeaveEvent.getContinueNavigationAction(),
                beforeLeaveEvent.getLocation().getPath());
        add(new Span("beforeLeave"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        log.info("onAttach");
        add(new Span("onAttach"));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        log.info("onDetach");
        add(new Span("onDetach"));
    }
}
