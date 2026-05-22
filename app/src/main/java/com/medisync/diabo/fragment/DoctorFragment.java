package com.medisync.diabo.fragment;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.medisync.diabo.BuildConfig;
import com.medisync.diabo.R;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.LabResult;
import com.medisync.diabo.model.MedicalReport;
import com.medisync.diabo.model.Medication;
import com.medisync.diabo.service.NotificationHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoctorFragment extends Fragment {
    private final ExecutorService reportExecutor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnDoctorReport = view.findViewById(R.id.btn_generate_doctor_report);
        if (btnDoctorReport != null) {
            btnDoctorReport.setOnClickListener(v -> generateAndShareReport("Doctor"));
        }

        Button btnFamilyReport = view.findViewById(R.id.btn_generate_family_report);
        if (btnFamilyReport != null) {
            btnFamilyReport.setOnClickListener(v -> generateAndShareReport("Family"));
        }
    }

    private void generateAndShareReport(String role) {
        Toast.makeText(requireContext(), "Generating report...", Toast.LENGTH_SHORT).show();
        reportExecutor.execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(requireContext());
                List<MedicalReport> reports = db.appDao().getAllReportsSync();
                List<LabResult> labs = db.appDao().getAllLabResultsSync();
                List<Medication> medications = db.appDao().getActiveMedicationsSync();

                if ((reports == null || reports.isEmpty()) && (labs == null || labs.isEmpty()) && (medications == null || medications.isEmpty())) {
                    postFailure("No report data found. Upload a report or add medications first.");
                    return;
                }

                File file = writeReportPdf(role, reports, labs, medications);
                Uri fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        file
                );

                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) showPdfReadyDialog(role, fileUri);
                });
            } catch (Exception e) {
                postFailure("Could not generate report: " + e.getMessage());
            }
        });
    }

    private File writeReportPdf(String role, List<MedicalReport> reports, List<LabResult> labs, List<Medication> medications) throws Exception {
        File dir = new File(requireContext().getCacheDir(), "generated_reports");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Report folder unavailable");
        }

        String fileName = "diabo_health_report_" + role.toLowerCase(Locale.US) + "_" + System.currentTimeMillis() + ".pdf";
        File file = new File(dir, fileName);
        List<String> lines = buildReportLines(role, reports, labs, medications);
        writePdf(file, lines);
        return file;
    }

    private void writePdf(File file, List<String> lines) throws Exception {
        PdfDocument document = new PdfDocument();
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF202124);
        textPaint.setTextSize(12f);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(0xFF000000);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(22f);

        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(0xFF402673);
        sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sectionPaint.setTextSize(15f);

        int pageWidth = 595;
        int pageHeight = 842;
        int margin = 40;
        int y = margin;
        int pageNumber = 1;

        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create());
        Canvas canvas = page.getCanvas();

        for (String line : lines) {
            Paint paint = line.startsWith("# ") ? titlePaint : line.startsWith("## ") ? sectionPaint : textPaint;
            String printable = line.replaceFirst("^##?\\s*", "");
            int lineHeight = paint == titlePaint ? 30 : paint == sectionPaint ? 24 : 18;

            for (String wrapped : wrap(printable, paint, pageWidth - (margin * 2))) {
                if (y > pageHeight - margin) {
                    document.finishPage(page);
                    pageNumber++;
                    page = document.startPage(new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create());
                    canvas = page.getCanvas();
                    y = margin;
                }
                canvas.drawText(wrapped, margin, y, paint);
                y += lineHeight;
            }
            y += line.trim().isEmpty() ? 8 : 4;
        }

        try (FileOutputStream output = new FileOutputStream(file)) {
            document.finishPage(page);
            document.writeTo(output);
        } finally {
            document.close();
        }
    }

    private List<String> buildReportLines(String role, List<MedicalReport> reports, List<LabResult> labs, List<Medication> medications) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        out.add("# DIABO HEALTH REPORT");
        out.add("Prepared for: " + role);
        out.add("Generated: " + dateTimeFormat.format(new Date()));
        out.add("");

        out.add("## LAB RESULTS");
        if (labs == null || labs.isEmpty()) {
            out.add("No lab results available.");
        } else {
            for (LabResult lab : labs) {
                out.add("- " + clean(lab.testName, "Unknown test")
                        + ": " + formatResult(lab)
                        + " | Range: " + clean(lab.normalRange, "See report")
                        + " | Status: " + clean(lab.status, "Not specified")
                        + " | Date: " + formatDate(lab.testDate));
            }
        }

        out.add("");
        out.add("## ACTIVE MEDICATIONS");
        if (medications == null || medications.isEmpty()) {
            out.add("No active medications available.");
        } else {
            for (Medication med : medications) {
                out.add("- " + clean(med.name, "Unnamed medication")
                        + " | Dosage: " + clean(med.dosage, "Not specified")
                        + " | Frequency: " + clean(med.frequency, "Not specified")
                        + " | Instructions: " + clean(med.instructions, "Not specified"));
            }
        }

        out.add("");
        out.add("## REPORT INSIGHTS");
        if (reports == null || reports.isEmpty()) {
            out.add("No uploaded report summaries available.");
        } else {
            for (MedicalReport report : reports) {
                out.add("- Report date: " + formatDate(report.getDisplayDate()));
                out.add(clean(report.aiInsights, "No AI insight saved for this report."));
                out.add("");
            }
        }

        out.add("Note: This report is generated from data stored in the Diabo app and is not a medical diagnosis.");
        return out;
    }

    private void showPdfReadyDialog(String role, Uri fileUri) {
        new AlertDialog.Builder(requireContext())
                .setTitle("PDF Report Ready")
                .setMessage("Preview the generated report before sharing, or share it directly.")
                .setPositiveButton("Preview", (dialog, which) -> previewReport(fileUri))
                .setNegativeButton("Share", (dialog, which) -> shareReport(role, fileUri))
                .setNeutralButton("Close", null)
                .show();
        NotificationHelper.showSuccess(getView(), "PDF report generated.");
    }

    private void previewReport(Uri fileUri) {
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setDataAndType(fileUri, "application/pdf");
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(viewIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No PDF viewer found on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReport(String role, Uri fileUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Diabo Health Report (" + role + ")");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Attached is my Diabo health report with lab results and active medications.");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share Generated Report"));
        NotificationHelper.showSuccess(getView(), "Report generated and ready to share.");
    }

    private List<String> wrap(String text, Paint paint, int maxWidth) {
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (paint.measureText(candidate) <= maxWidth) {
                current = new StringBuilder(candidate);
            } else {
                if (current.length() > 0) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }

    private void postFailure(String message) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            NotificationHelper.showError(getView(), message);
        });
    }

    private String formatResult(LabResult lab) {
        String value = clean(lab.stringValue, "");
        if (value.isEmpty()) {
            value = String.valueOf(lab.value);
            if (value.endsWith(".0")) value = value.substring(0, value.length() - 2);
        }
        String unit = clean(lab.unit, "");
        return unit.isEmpty() ? value : value + " " + unit;
    }

    private String formatDate(Date date) {
        return date == null ? "Not specified" : dateTimeFormat.format(date);
    }

    private String clean(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) return fallback;
        return trimmed;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        reportExecutor.shutdown();
    }
}
