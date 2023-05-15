package org.rsinitsyn.quiz.page;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.rsinitsyn.quiz.component.MainLayout;
import org.vaadin.addons.pandalyte.VoiceRecognition;

@Route(value = "/fonts", layout = MainLayout.class)
@PageTitle("Fonts")
public class FontsPage extends VerticalLayout {

    public FontsPage() {
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
    }

    private Span dummyText(String fontSize, String fontWeight) {
        Span span = new Span();
        span.setText(fontSize + " - " + fontWeight + ". Lorem Ipsum is simply dummy text of the printing");
        span.addClassNames(fontSize, fontWeight, LumoUtility.LineHeight.XSMALL);
        span.addComponentAsFirst(new Hr());
        return span;
    }
}
