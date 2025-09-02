package com.elfstack.toys.security.dev;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

@PageTitle("Sistem Deteksi Penyakit Unggas Menggunakan Metode Certainty Factor")
@AnonymousAllowed
// No @Route annotation - the route is registered dynamically by DevSecurityConfig.
class DevLoginView extends Main implements BeforeEnterObserver {

    static final String LOGIN_PATH = "dev-login";

    private final AuthenticationContext authenticationContext;
    private final LoginForm login;

    DevLoginView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        // ===== Root layout =====
        setSizeFull();
        addClassName("dev-login-view");
        // Background gradient yang lembut + center content
        getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "24px")
                .set("background", "linear-gradient(135deg, var(--lumo-tint-5pct), var(--lumo-contrast-5pct))");

        // ===== Card container =====
        Div card = new Div();
        card.addClassName("dev-login-card");
        card.getStyle()
                .set("width", "100%")
                .set("max-width", "460px")
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("box-shadow", "0 10px 30px rgba(0,0,0,0.12)")
                .set("padding", "28px")
                .set("box-sizing", "border-box");

        // ===== Brand / header =====
        Image logo = new Image(
                "https://tse1.mm.bing.net/th/id/OIP.97UHx3v-fOjQmfpvNB687AHaEf?r=0&pid=Api",
                "Logo Unggas"
        );
        logo.setWidth("120px");
        logo.getStyle()
                .set("display", "block")
                .set("margin", "0 auto 16px");

        H1 title = new H1("Sistem Deteksi Penyakit Unggas");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "22px")
                .set("text-align", "center");

        H1 title2 = new H1("Poultry Scan App");
        title2.getStyle()
                .set("margin", "0")
                .set("font-size", "22px")
                .set("text-align", "center");


        H4 subtitle = new H4("Metode Certainty Factor (CF)");
        subtitle.getStyle()
                .set("margin", "4px 0 18px 0")
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500");

        // ===== Login form =====
        login = new LoginForm();
        login.setAction(LOGIN_PATH);
        login.setForgotPasswordButtonVisible(false);
        login.getStyle()
                .set("width", "100%")
                .set("margin-top", "8px");

        // Label bantuan akun contoh
        Div exampleUsersHeader = new Div(new Span("Gunakan kredensial berikut untuk masuk:"));
        exampleUsersHeader.getStyle()
                .set("margin", "18px 0 8px 0")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        Div exampleUsers = new Div();
        exampleUsers.addClassName("dev-users");
        exampleUsers.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr")
                .set("gap", "10px");

        SampleUsers.ALL_USERS.forEach(user -> exampleUsers.add(createSampleUserCard(user)));

        // Footer kecil
        Paragraph foot = new Paragraph("© " + java.time.Year.now() + " Sistem CF Unggas");
        foot.getStyle()
                .set("margin", "16px 0 0 0")
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        // Susun ke card
        card.add(logo, title, title2, subtitle, login, exampleUsersHeader, exampleUsers, foot);

        // Tambah ke root
        add(card);
    }

    private Component createSampleUserCard(DevUser user) {
        Div card = new Div();
        card.addClassName("dev-user-card");
        card.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr auto")
                .set("align-items", "center")
                .set("gap", "8px")
                .set("padding", "10px 12px")
                .set("border-radius", "12px")
                .set("background", "var(--lumo-contrast-5pct)");

        H3 fullName = new H3(user.getAppUser().getFullName());
        fullName.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-m)");

        DescriptionList credentials = new DescriptionList();
        credentials.add(new DescriptionList.Term("Nama Pengguna"),
                new DescriptionList.Description(user.getUsername()));
        credentials.add(new DescriptionList.Term("Kata Sandi"),
                new DescriptionList.Description(SampleUsers.SAMPLE_PASSWORD));
        credentials.getStyle()
                .set("margin", "2px 0 0 0")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        Div left = new Div(fullName, credentials);
        left.getStyle().set("display", "grid").set("gap", "2px");

        Button loginButton = new Button(VaadinIcon.SIGN_IN.create(), event -> {
            login.getElement().executeJs("""
                document.getElementById("vaadinLoginUsername").value = $0;
                document.getElementById("vaadinLoginPassword").value = $1;
                document.forms[0].submit();
            """, user.getUsername(), SampleUsers.SAMPLE_PASSWORD);
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        loginButton.getElement().setProperty("title", "Masuk sebagai " + user.getAppUser().getFullName());
        loginButton.getStyle().set("margin-left", "6px");

        card.add(left, loginButton);
        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            event.forwardTo("/tes-gejala");
            return;
        }
        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
