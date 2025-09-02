package com.elfstack.toys.base.ui.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.theme.lumo.LumoUtility.*;

public final class ViewToolbar extends Composite<Header> {

    public ViewToolbar(String viewTitle, Component... components) {
        addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, AlignItems.STRETCH, Gap.MEDIUM,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);

        var drawerToggle = new DrawerToggle();
        drawerToggle.addClassNames(Margin.NONE);

        var title = new H1(viewTitle);
        title.addClassNames(FontSize.XLARGE, Margin.NONE, FontWeight.LIGHT);

        var toggleAndTitle = new Div(drawerToggle, title);
        toggleAndTitle.addClassNames(Display.FLEX, AlignItems.CENTER);

        // 🔗 Navbar / Link ke halaman diagnosa (/tes-gejala)
        Anchor diagnosaLink = new Anchor("/tes-gejala", "Diagnosa");
        diagnosaLink.getElement().setAttribute("aria-label", "Pergi ke halaman diagnosa");
        diagnosaLink.addClassNames(FontSize.MEDIUM, Padding.SMALL, BorderRadius.MEDIUM);
        // opsional gaya tombol halus
        diagnosaLink.getStyle()
                .set("text-decoration", "none")
                .set("color", "var(--lumo-primary-text-color)")
                .set("background", "var(--lumo-primary-color-10pct)");

        // container kanan (navbar + actions jika ada)
        var rightSide = new Div();
        rightSide.addClassNames(Display.FLEX, AlignItems.CENTER, Gap.MEDIUM);

        rightSide.add(diagnosaLink);

        if (components.length > 0) {
            var actions = new Div(components);
            actions.addClassNames(Display.FLEX, FlexDirection.COLUMN, JustifyContent.BETWEEN, Flex.GROW, Gap.SMALL,
                    FlexDirection.Breakpoint.Medium.ROW);
            rightSide.add(actions);
        }

        // susun ke header
        var headerRow = new Div(toggleAndTitle, rightSide);
        headerRow.addClassNames(Display.FLEX, AlignItems.CENTER, JustifyContent.BETWEEN, Width.FULL);
        getContent().add(headerRow);
    }

    public static Component group(Component... components) {
        var group = new Div(components);
        group.addClassNames(Display.FLEX, FlexDirection.COLUMN, AlignItems.STRETCH, Gap.SMALL,
                FlexDirection.Breakpoint.Medium.ROW, AlignItems.Breakpoint.Medium.CENTER);
        return group;
    }
}
