package com.elfstack.toys.base.ui.view;

import com.elfstack.toys.taskmanagement.ui.view.TaskListView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

/**
 * Root ('/') view.
 */
@Route
@PermitAll
public final class MainView extends Main {

    public MainView() {
        addClassName(LumoUtility.Padding.MEDIUM);

        // ===== NAVBAR (di sini, bukan di ViewToolbar) =====
        Div navbar = new Div();
        navbar.getStyle()
                .set("display", "flex")
                .set("alignItems", "center")
                .set("justifyContent", "space-between")
                .set("gap", "16px")
                .set("padding", "8px 12px")
                .set("border-radius", "12px")
                .set("background", "var(--lumo-contrast-5pct)");

        H1 brand = new H1("Sistem Deteksi Penyakit Unggas");
        brand.getStyle().set("font-size", "1.25rem").set("margin", "0");

        Div navLinks = new Div();
        navLinks.getStyle().set("display", "flex").set("gap", "12px").set("alignItems", "center");

        Anchor toDiagnosa = new Anchor("/tes-gejala", "Diagnosa");
        toDiagnosa.getStyle()
                .set("text-decoration", "none")
                .set("padding", "6px 10px")
                .set("border-radius", "10px")
                .set("background", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)");

        Anchor toHome = new Anchor("/", "Beranda");
        toHome.getStyle()
                .set("text-decoration", "none")
                .set("padding", "6px 10px")
                .set("border-radius", "10px")
                .set("color", "var(--lumo-body-text-color)");

        navLinks.add(toHome, toDiagnosa);
        navbar.add(brand, navLinks);

        // ===== SECTION GEJALA (tampilkan gejalanya) =====
        Paragraph subtitle = new Paragraph("Daftar Gejala yang tersedia:");
        subtitle.getStyle().set("margin", "16px 0 8px 0");

        Paragraph subtitle2 = new Paragraph("Daftar Gejala yang tersedia:");
        subtitle2.getStyle().set("margin", "16px 0 8px 0");

        List<String> gejala = Arrays.asList("G1 - Ngorok/nafas berbunyi","G2 - Hidung berlendir","G3 - Diare hijau","G4 - Lesu dan nafsu makan menurun","G5 - Bulu kusam dan berdiri","G6 - Jengger kebiruan","G7 - Penurunan produksi telur","G8 - Kelumpuhan kaki/sayap" , "G9 - Kematian mendadak","G10 - Pembengkakan kepala/wajah");
        List<String> disease = Arrays.asList("Newcastle Disease", "Avian Influenza","Infectious Bronchitis");


        Div gejalaWrap = new Div();
        gejalaWrap.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "8px");

        for (String g : gejala) {
            Div chip = new Div(g);
            chip.getStyle()
                    .set("padding", "6px 10px")
                    .set("border-radius", "999px")
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-body-text-color)");
            gejalaWrap.add(chip);
        }

        Div diseaseWrap = new Div();
        diseaseWrap.getStyle()
                .set("display", "flex")
                .set("flex-wrap", "wrap")
                .set("gap", "8px");

        for (String g : disease) {
            Div chip = new Div(g);
            chip.getStyle()
                    .set("padding", "6px 10px")
                    .set("border-radius", "999px")
                    .set("background", "var(--lumo-contrast-10pct)")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("color", "var(--lumo-body-text-color)");
            diseaseWrap.add(chip);
        }

        // ===== CTA ke Diagnosa =====
        // ===== CTA ke Diagnosa =====
        Div cta = new Div();
        cta.getStyle().set("margin-top", "12px");
        Anchor startDiag = new Anchor("/tes-gejala", "Mulai Diagnosa Sekarang →");
        startDiag.getStyle()
                .set("text-decoration", "none")
                .set("padding", "10px 14px")
                .set("border-radius", "12px")
                .set("background", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)");
        cta.add(startDiag);

// ===== Tombol untuk buka PDF =====
        Div pdfSection = new Div();
        pdfSection.getStyle().set("margin-top", "32px"); // jarak lebih besar biar jelas

        Anchor openPdf = new Anchor(
                "https://drive.google.com/file/d/1Cu0jq5I6qf-X-jzxfLRyEZldKDu8Jl6l/view?usp=sharing",
                "Buka PDF Laporan Tugas Akhir Nodemation (N8N)"
        );
        openPdf.setTarget("_blank"); // buka di tab baru
        openPdf.getStyle()
                .set("text-decoration", "none")
                .set("padding", "10px 14px")
                .set("border-radius", "12px")
                .set("background", "var(--lumo-primary-color)")
                .set("color", "var(--lumo-primary-contrast-color)");

        pdfSection.add(openPdf);


        add(navbar, subtitle, gejalaWrap, subtitle2, diseaseWrap, cta,pdfSection);
    }



    /** Navigates to the main view. */
    public static void showMainView() {
        UI.getCurrent().navigate(TaskListView.class);
    }
}
