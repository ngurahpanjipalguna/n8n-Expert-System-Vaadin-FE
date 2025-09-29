package com.elfstack.toys.taskmanagement.ui.view;

import com.elfstack.toys.base.ui.view.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.impl.JreJsonFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@Route(value = "/tes-gejala", layout = MainLayout.class)
@AnonymousAllowed
public class TaskListView extends VerticalLayout {

    private final Grid<ResultModel> grid = new Grid<>(ResultModel.class);
    private final MultiSelectComboBox<String> gejalaBox = new MultiSelectComboBox<>();
    private final IntegerField birdCountField = new IntegerField("Jumlah Unggas");
    private final  Button exportExcelBtn = new Button("⬇️ Ekspor Excel", e -> exportToExcel());

    private final TextField phoneField = new TextField("Nomor WhatsApp (contoh: 6281234567890)");

    private List<ResultModel> lastResults = new ArrayList<>();
    private Set<String> lastSelectedGejala = new HashSet<>();
    private int lastBirdCount = 0;

    private static final double ND_COST_PER_BIRD = 11.0;
    private static final double AI_COST_PER_BIRD = 8.0;
    private static final double IB_COST_PER_BIRD = 4.0;

    // Map kode gejala → deskripsi
    private static final Map<String, String> GEJALA_MAP = new LinkedHashMap<>();
    static {
        GEJALA_MAP.put("G1", "Ngorok/nafas berbunyi");
        GEJALA_MAP.put("G2", "Hidung berlendir");
        GEJALA_MAP.put("G3", "Diare hijau");
        GEJALA_MAP.put("G4", "Lesu dan nafsu makan menurun");
        GEJALA_MAP.put("G5", "Bulu kusam dan berdiri");
        GEJALA_MAP.put("G6", "Jengger kebiruan");
        GEJALA_MAP.put("G7", "Penurunan produksi telur");
        GEJALA_MAP.put("G8", "Kelumpuhan kaki/sayap");
        GEJALA_MAP.put("G9", "Kematian mendadak");
        GEJALA_MAP.put("G10","Pembengkakan kepala/wajah");
    }

    public TaskListView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Dropdown gejala
        gejalaBox.setLabel("Pilih Gejala");
        gejalaBox.setItems(GEJALA_MAP.entrySet().stream()
                .map(e -> e.getKey() + " - " + e.getValue())
                .toList());
        gejalaBox.setPlaceholder("Pilih gejala...");

        // Input jumlah unggas
        birdCountField.setMin(10);
        birdCountField.setMax(100);
        birdCountField.setStep(1);
        birdCountField.setValue(10);

        // Input nomor WhatsApp
        phoneField.setWidth("300px");

        // Tombol aksi
        Button fetchButton = new Button("🔍 Diagnosa", e -> fetchData());
        Button sendWaButton = new Button("📲 Kirim ke WA", e -> sendToWhatsApp());
        HorizontalLayout actions = new HorizontalLayout(fetchButton, sendWaButton,exportExcelBtn);

        // Grid hasil diagnosa
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setColumns("disease", "cfFinal", "level", "costUsdFormatted", "costIdrFormatted");
        grid.getColumnByKey("disease").setHeader("Disease");
        grid.getColumnByKey("cfFinal").setHeader("CF Final");
        grid.getColumnByKey("level").setHeader("Level");
        grid.getColumnByKey("costUsdFormatted").setHeader("Biaya (USD)");
        grid.getColumnByKey("costIdrFormatted").setHeader("Biaya (IDR)");
        grid.addItemClickListener(event -> showDetailDialog(event.getItem()));
        grid.setWidthFull();

        add(gejalaBox, birdCountField, phoneField, actions, grid);
    }

    private void showDetailDialog(ResultModel model) {
        Dialog dialog = new Dialog();
        dialog.setWidth("500px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout layout = new VerticalLayout();
        layout.add(new H3("Detail Penyakit"));
        layout.add(new Paragraph("Nama Disease : " + model.getDisease()));
        layout.add(new Paragraph("CF Final     : " + String.format(Locale.US, "%.2f", model.getCfFinal())));
        layout.add(new Paragraph("Level        : " + model.getLevel()));
        layout.add(new Paragraph("Biaya (USD)  : " + model.getCostUsdFormatted()));
        layout.add(new Paragraph("Biaya (IDR)  : " + model.getCostIdrFormatted()));

        String explanation = getDummyExplanation(model.getDisease());
        layout.add(new H3("Penjelasan"));
        layout.add(new Paragraph(explanation));

        Button qrButton = new Button("💳 Buat QR Code Pembayaran", ev -> {
            double amount = model.getCostIdr();
            showQrCodeDialog(amount);
        });
        Button closeBtn = new Button("Tutup", ev -> dialog.close());

        layout.add(qrButton, closeBtn);

        dialog.add(layout);
        dialog.open();
    }

    private void exportToExcel() {
        if (lastResults.isEmpty()) {
            Notification.show("Belum ada data untuk diekspor.", 3000, Notification.Position.MIDDLE);
            return;
        }

        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Diagnosa Unggas");

            // Row 0 → Judul + tanggal/waktu
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
            org.apache.poi.ss.usermodel.Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("📋 Data Diagnosa Unggas - Ekspor pada " + timestamp);

            // Row 2 → Header kolom
            String[] headers = {"Disease", "CF Final", "Level", "Biaya (USD)", "Biaya (IDR)"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Data mulai row ke-3
            int rowIdx = 3;
            for (ResultModel r : lastResults) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getDisease());
                row.createCell(1).setCellValue(r.getCfFinal());
                row.createCell(2).setCellValue(r.getLevel());
                row.createCell(3).setCellValue(r.getCostUsdFormatted());
                row.createCell(4).setCellValue(r.getCostIdrFormatted());
            }

            // Autosize kolom
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Simpan ke stream → download
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            workbook.write(bos);
            byte[] excelBytes = bos.toByteArray();

            // Trigger download di Vaadin
            com.vaadin.flow.component.html.Anchor download = new com.vaadin.flow.component.html.Anchor(
                    new com.vaadin.flow.server.StreamResource("diagnosa-" + timestamp.replace(" ", "_").replace(":", "-") + ".xlsx",
                            () -> new java.io.ByteArrayInputStream(excelBytes)),
                    "Klik di sini untuk mengunduh file"
            );
            download.getElement().setAttribute("download", true);

            Dialog dialog = new Dialog(new Paragraph("File Excel siap diunduh."), download);
            dialog.open();

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Gagal mengekspor ke Excel.", 3000, Notification.Position.MIDDLE);
        }
    }


    private void showQrCodeDialog(double amount) {
        try {
            URL url = new URL("https://api.mayar.id/hl/v1/qrcode/create");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOiIyYjE3OWI0MC0zNWNmLTRlZDEtOTQzOS00ZjNmNmMyZDU0MmUiLCJhY2NvdW50SWQiOiI0OTU5NTk1ZC1iMzY0LTQ5MGItYmQzMS1lNjQ5ZWUwNjU5Y2YiLCJjcmVhdGVkQXQiOiIxNzU2NDI4NzM4NzY1Iiwicm9sZSI6ImRldmVsb3BlciIsInN1YiI6InBhbmppX3BhbGd1bmFAc3R1ZGVudC51bnVkLmFjLmlkIiwibmFtZSI6IkhlcmJhdGVjaCIsImxpbmsiOiJwYW5qaS1wYWxndW5hIiwiaXNTZWxmRG9tYWluIjpudWxsLCJpYXQiOjE3NTY0Mjg3Mzh9.XrwU9LMJ3zioUoRztbMOe-0irn9RQvLek7A2jqQAVzy3y7a8-R50AT3fCTfNkKZ5jHAlSh-D8ml0TM-B-0QdXeLLrtkYFsAQ9MC4WuJDjKj8zvrrGO-I3xwUt3rMS0M3OJtLFhH2NGhrcQlloldRFR4ihfrzzHm53-e0BPzovZYFPPpvhQ6jZZ4jD5QfK4in2_8_4PdGontJZYaijzQQoWn3TqWaVzrYlWNTd9adIj31Q364SrrfuSoy-9jzYZe5siG4puKgG4QsvVDG8gW1D28YeSu7BCz0uNAUd_4mjLM8o84UfgiZ0WnB1rEiJDB_QmCgEt5BjrofmM529wyH1A");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = "{ \"amount\": " + (int) Math.round(amount) + " }";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            try (Scanner sc = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                while (sc.hasNext()) response.append(sc.nextLine());
            }

            JreJsonFactory factory = new JreJsonFactory();
            JsonObject jsonResp = (JsonObject) factory.parse(response.toString());
            JsonObject data = jsonResp.getObject("data");
            String qrUrl = data.getString("url");

            Dialog qrDialog = new Dialog();
            qrDialog.setWidth("400px");
            qrDialog.setCloseOnEsc(true);
            qrDialog.setCloseOnOutsideClick(true);

            Image qrImage = new Image(qrUrl, "QR Code");
            qrImage.setWidth("300px");

            qrDialog.add(new H3("Scan untuk Bayar"), qrImage);
            qrDialog.open();

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Gagal membuat QR Code", 3000, Notification.Position.MIDDLE);
        }
    }

    private String getDummyExplanation(String disease) {
        String d = disease.toUpperCase(Locale.ROOT);
        if (d.contains("NEWCASTLE")) {
            return "Newcastle Disease (ND) adalah penyakit menular pada unggas yang disebabkan oleh virus paramyxovirus. "
                    + "Penanganan biasanya melalui vaksinasi preventif.";
        } else if (d.contains("AVIAN") || d.contains("AI")) {
            return "Avian Influenza (AI), atau flu burung, adalah penyakit akibat virus influenza tipe A. "
                    + "AI sangat menular dan dapat menimbulkan kematian massal pada unggas.";
        } else if (d.contains("BRONCHITIS") || d.contains("IB")) {
            return "Infectious Bronchitis (IB) adalah penyakit pernapasan akibat coronavirus yang menyerang ayam.";
        }
        return "Informasi detail penyakit ini belum tersedia.";
    }

    private void fetchData() {
        try {
            lastSelectedGejala = gejalaBox.getValue();
            lastBirdCount = birdCountField.getValue();

            if (lastSelectedGejala.isEmpty()) {
                Notification.show("Pilih minimal satu gejala.", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (lastBirdCount < 10 || lastBirdCount > 100) {
                Notification.show("Jumlah unggas harus antara 10–100.", 3000, Notification.Position.MIDDLE);
                return;
            }

            // ambil hanya kode (G1, G2, dst)
            String selectedStr = lastSelectedGejala.stream()
                    .map(item -> "\"" + item.split(" - ")[0] + "\"")
                    .collect(Collectors.joining(","));
            String payload = "{ \"selected\": [" + selectedStr + "] }";

            URL url = new URL("http://38.3.160.20:5677/webhook/cf-unggas-multi");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            StringBuilder response = new StringBuilder();
            try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8)) {
                while (scanner.hasNext()) response.append(scanner.nextLine());
            }

            JreJsonFactory factory = new JreJsonFactory();
            JsonObject jsonResponse = (JsonObject) factory.parse(response.toString());
            JsonArray results = jsonResponse.getArray("results");

            List<ResultModel> rows = new ArrayList<>();
            for (int i = 0; i < results.length(); i++) {
                JsonObject obj = results.getObject(i);
                String disease = obj.getString("disease");
                double cfFinal = obj.getNumber("CF_Final");
                String level = obj.getString("level");

                double costPerBird = mapCostPerBirdUSD(disease);
                double costUsd = costPerBird * lastBirdCount;
                double costIdr = convertToIdr(costUsd);

                rows.add(new ResultModel(disease, cfFinal, level, costUsd, costIdr));
            }

            lastResults = rows;
            grid.setItems(rows);

        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Terjadi kesalahan saat memproses data.", 3000, Notification.Position.MIDDLE);
        }
    }

    private void sendToWhatsApp() {
        if (lastResults.isEmpty()) {
            Notification.show("Belum ada hasil diagnosa.", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (phoneField.isEmpty()) {
            Notification.show("Masukkan nomor WhatsApp tujuan.", 3000, Notification.Position.MIDDLE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📋 Diagnosa Unggas\n");
        // 👉 tampilkan gejala lengkap
        sb.append("Gejala dipilih:\n");
        for (String g : lastSelectedGejala) {
            sb.append("• ").append(g).append("\n");
        }
        sb.append("\nJumlah unggas: ").append(lastBirdCount).append("\n\n");
        sb.append("Hasil:\n");

        for (ResultModel r : lastResults) {
            sb.append("- ")
                    .append(r.getDisease())
                    .append(" | CF: ").append(String.format("%.2f", r.getCfFinal()))
                    .append(" | Level: ").append(r.getLevel())
                    .append(" | USD: ").append(r.getCostUsdFormatted())
                    .append(" | IDR: ").append(r.getCostIdrFormatted())
                    .append("\n");
        }

        try {
            String pesan = URLEncoder.encode(sb.toString(), StandardCharsets.UTF_8);
            String nomorTujuan = phoneField.getValue().trim();
            String url = "https://wa.me/" + nomorTujuan + "?text=" + pesan;

            getUI().ifPresent(ui -> ui.getPage().open(url));
        } catch (Exception e) {
            e.printStackTrace();
            Notification.show("Gagal membentuk link WA.", 3000, Notification.Position.MIDDLE);
        }
    }


    private double mapCostPerBirdUSD(String diseaseName) {
        if (diseaseName == null) return 0.0;
        String d = diseaseName.toUpperCase(Locale.ROOT);
        if (d.contains("NEWCASTLE")) return ND_COST_PER_BIRD;
        if (d.contains("AVIAN") || d.contains("AI")) return AI_COST_PER_BIRD;
        if (d.contains("BRONCHITIS") || d.contains("IB")) return IB_COST_PER_BIRD;
        return 0.0;
    }
    private double convertToIdr(double usdAmount) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL("http://38.3.160.20:5677/webhook/convert-currency");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            int amountInt = (int) Math.round(usdAmount);
            String payload = "{ \"from\":\"USD\", \"to\":\"IDR\", \"amount\":" + amountInt + " }";

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            InputStream is = (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300)
                    ? conn.getInputStream() : conn.getErrorStream();
            String resp = readAll(is);

            JreJsonFactory factory = new JreJsonFactory();
            elemental.json.JsonValue parsed = factory.parse(resp);

            if (parsed instanceof JsonObject) {
                JsonObject obj = (JsonObject) parsed;
                if (obj.hasKey("result")) return obj.getNumber("result");
                if (obj.hasKey("data")) {
                    JsonObject data = obj.getObject("data");
                    if (data != null && data.hasKey("result")) return data.getNumber("result");
                }
            } else if (parsed instanceof JsonArray) {
                JsonArray arr = (JsonArray) parsed;
                if (arr.length() > 0) {
                    if (arr.get(0) instanceof JsonObject) {
                        JsonObject first = arr.getObject(0);
                        if (first.hasKey("result")) return first.getNumber("result");
                        if (first.hasKey("data")) {
                            JsonObject data = first.getObject("data");
                            if (data != null && data.hasKey("result")) return data.getNumber("result");
                        }
                    } else if (arr.get(0).getType() == elemental.json.JsonType.NUMBER) {
                        return arr.get(0).asNumber();
                    }
                }
            }

            // fallback parse angka mentah
            String digits = resp.replaceAll("[^0-9.]", "");
            if (!digits.isEmpty()) return Double.parseDouble(digits);

            System.err.println("convertToIdr: tidak bisa parse response: " + resp);
            return 0.0;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }



    private String readAll(InputStream is) throws Exception {
        if (is == null) return "";
        try (Scanner sc = new Scanner(is, StandardCharsets.UTF_8)) {
            sc.useDelimiter("\\A");
            return sc.hasNext() ? sc.next() : "";
        }
    }

    // ===== Model untuk Grid =====
    public static class ResultModel {
        private final String disease;
        private final double cfFinal;
        private final String level;
        private final double costUsd;
        private final double costIdr;

        public ResultModel(String disease, double cfFinal, String level, double costUsd, double costIdr) {
            this.disease = disease;
            this.cfFinal = cfFinal;
            this.level = level;
            this.costUsd = costUsd;
            this.costIdr = costIdr;
        }

        public String getDisease() { return disease; }
        public double getCfFinal() { return cfFinal; }
        public String getLevel() { return level; }
        public double getCostUsd() { return costUsd; }
        public double getCostIdr() { return costIdr; }

        public String getCostUsdFormatted() {
            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            return "$" + nf.format((int) Math.round(costUsd));
        }

        public String getCostIdrFormatted() {
            DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("id", "ID"));
            df.applyPattern("#,###");
            return "Rp " + df.format((long) Math.round(costIdr));
        }
    }
}
