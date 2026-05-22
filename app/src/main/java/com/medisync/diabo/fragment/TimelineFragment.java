package com.medisync.diabo.fragment;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.medisync.diabo.R;
import com.medisync.diabo.ReportUploadActivity;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.LabResult;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimelineFragment extends Fragment {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timeline, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View loading = view.findViewById(R.id.pb_timeline_loading);
        View emptyState = view.findViewById(R.id.layout_timeline_empty);
        View content = view.findViewById(R.id.scroll_timeline_content);
        View btnUpload = view.findViewById(R.id.btn_empty_upload);

        // Wire "Upload Report" empty-state CTA
        if (btnUpload != null) {
            btnUpload.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), ReportUploadActivity.class);
                startActivity(intent);
            });
        }

        // Show loading spinner while waiting for first LiveData emission
        if (loading != null) loading.setVisibility(View.VISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        if (content != null) content.setVisibility(View.GONE);

        AppDatabase.getInstance(requireContext()).appDao().getAllLabResults().observe(getViewLifecycleOwner(), labs -> {
            if (loading != null) loading.setVisibility(View.GONE);
            if (labs == null) labs = new ArrayList<>();

            if (labs.isEmpty()) {
                // Show empty state, hide content
                if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                if (content != null) content.setVisibility(View.GONE);
            } else {
                // Show content, hide empty state
                if (emptyState != null) emptyState.setVisibility(View.GONE);
                if (content != null) content.setVisibility(View.VISIBLE);
                renderTimeline(view, labs);
            }
        });
    }

    private void renderTimeline(View root, List<LabResult> labs) {
        TextView date = root.findViewById(R.id.tv_timeline_date);
        LinearLayout primary = root.findViewById(R.id.container_primary_organ);
        LinearLayout summary = root.findViewById(R.id.container_summary);
        LinearLayout cards = root.findViewById(R.id.container_organ_cards);
        primary.removeAllViews();
        summary.removeAllViews();
        cards.removeAllViews();

        Map<String, List<LabResult>> byOrgan = groupByOrgan(labs);
        String firstDate = dateFormat.format(labs.get(0).testDate == null ? new Date() : labs.get(0).testDate);
        date.setText(firstDate);
        date.setOnClickListener(v -> showReportHistory());

        String primaryOrgan = byOrgan.keySet().iterator().next();
        addOrganDetailCard(primary, primaryOrgan, byOrgan.get(primaryOrgan));

        for (Map.Entry<String, List<LabResult>> entry : byOrgan.entrySet()) {
            addSummaryRow(summary, entry.getKey(), groupStatus(entry.getValue()));
            if (!entry.getKey().equals(primaryOrgan)) addOrganCompactCard(cards, entry.getKey(), entry.getValue());
        }
    }

    private Map<String, List<LabResult>> groupByOrgan(List<LabResult> labs) {
        LinkedHashMap<String, List<LabResult>> map = new LinkedHashMap<>();
        for (LabResult l : labs) {
            String organ = inferOrgan(l.testName);
            if (!map.containsKey(organ)) map.put(organ, new ArrayList<>());
            map.get(organ).add(l);
        }
        return map;
    }

    private String inferOrgan(String testName) {
        String t = testName == null ? "" : testName.toLowerCase(Locale.US);
        if (t.contains("alt") || t.contains("ast")) return "LIVER";
        if (t.contains("glucose") || t.contains("hba1c")) return "PANCREAS";
        if (t.contains("urine") || t.contains("creatinine")) return "KIDNEYS";
        if (t.contains("pressure") || t.contains("heart")) return "HEART";
        if (t.contains("iron") || t.contains("hemoglobin")) return "IRON STORES";
        if (t.contains("vitamin")) return "VITAMINS";
        return "BLOOD";
    }

    private String groupStatus(List<LabResult> items) {
        int bad = 0, warn = 0;
        for (LabResult item : items) {
            String s = safe(item.status);
            if (s.contains("abnormal") || s.contains("high") || s.contains("low")) bad++;
            else if (s.contains("borderline")) warn++;
        }
        if (bad > 0) return "ABNORMAL";
        if (warn > 0) return "BORDERLINE";
        return "NORMAL";
    }

    private void addOrganDetailCard(LinearLayout parent, String organ, List<LabResult> items) {
        addTitleRow(parent, organ, items.size(), groupStatus(items));
        addHeader(parent);
        for (LabResult item : items) addMetricRow(parent, item);
        int out = 0;
        for (LabResult item : items) if (!safe(item.status).contains("normal")) out++;
        if (out > 0) {
            TextView warn = new TextView(requireContext());
            warn.setText(organ + " CLINICAL STATUS\n" + out + " parameters outside reference range");
            warn.setTextColor(ContextCompat.getColor(requireContext(), R.color.vitalRed));
            warn.setTypeface(Typeface.DEFAULT_BOLD);
            warn.setTextSize(15f);
            warn.setPadding(dp(12), dp(14), dp(12), dp(8));
            parent.addView(warn);
        }
    }

    private void addTitleRow(LinearLayout parent, String organ, int count, String status) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(10));
        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(organIconRes(organ));
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setBackgroundResource(organIconBg(organ));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        iconParams.rightMargin = dp(12);
        row.addView(icon, iconParams);

        TextView left = new TextView(requireContext());
        left.setText(organ + "\n" + count + " parameters analyzed");
        left.setTypeface(Typeface.DEFAULT_BOLD);
        left.setTextSize(16f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView right = chip(status);
        row.addView(left);
        row.addView(right);
        parent.addView(row);
    }

    private void addHeader(LinearLayout parent) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(18), 0, dp(8));
        addHeaderCell(row, "Test", 1.35f);
        addHeaderCell(row, "Result", 1.05f);
        addHeaderCell(row, "Range", 0.9f);
        Space(row, dp(12));
        parent.addView(row);
    }

    private void addHeaderCell(LinearLayout row, String label, float weight) {
        TextView h = new TextView(requireContext());
        h.setText(label);
        h.setTextColor(ContextCompat.getColor(requireContext(), R.color.graySubtext));
        h.setTextSize(12f);
        h.setSingleLine(true);
        h.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        row.addView(h);
    }

    private void addMetricRow(LinearLayout parent, LabResult item) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(9), 0, dp(9));

        TextView t1 = new TextView(requireContext());
        t1.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.35f));
        t1.setText(cleanDisplay(item.testName, "Unknown test"));
        t1.setTextColor(ContextCompat.getColor(requireContext(), R.color.grayText));
        t1.setTextSize(14f);
        t1.setMaxLines(2);

        TextView t2 = new TextView(requireContext());
        t2.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.05f));
        t2.setTypeface(Typeface.DEFAULT_BOLD);
        t2.setTextColor(ContextCompat.getColor(requireContext(), colorForStatus(item.status)));
        t2.setText(formatResult(item));
        t2.setTextSize(14f);
        t2.setMaxLines(2);

        TextView t3 = new TextView(requireContext());
        t3.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f));
        t3.setText(cleanDisplay(item.normalRange, "See Report"));
        t3.setTextColor(ContextCompat.getColor(requireContext(), R.color.graySubtext));
        t3.setTextSize(13f);
        t3.setMaxLines(2);

        View dot = new View(requireContext());
        LinearLayout.LayoutParams d = new LinearLayout.LayoutParams(dp(8), dp(8));
        d.leftMargin = dp(4);
        dot.setLayoutParams(d);
        dot.setBackgroundResource(dotForStatus(item.status));

        row.addView(t1);
        row.addView(t2);
        row.addView(t3);
        row.addView(dot);
        parent.addView(row);
    }

    private void addSummaryRow(LinearLayout parent, String organ, String status) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView name = new TextView(requireContext());
        name.setText(organ);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setTextSize(16f);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(name);
        row.addView(chip(status));
        row.setPadding(0, dp(11), 0, dp(11));
        parent.addView(row);
    }

    private void addOrganCompactCard(LinearLayout parent, String organ, List<LabResult> items) {
        CardView cv = new CardView(requireContext());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = dp(10);
        cv.setLayoutParams(p);
        cv.setRadius(16f);
        cv.setCardElevation(0f);
        cv.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.timeline_card_border));
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(android.view.Gravity.CENTER_VERTICAL);
        box.setPadding(dp(18), dp(16), dp(18), dp(16));
        TextView left = new TextView(requireContext());
        left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        left.setText(organ + "\n" + items.size() + " parameters analyzed");
        left.setTypeface(Typeface.DEFAULT_BOLD);
        left.setTextSize(15f);
        left.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
        box.addView(left);
        box.addView(chip(groupStatus(items)));
        cv.addView(box);
        parent.addView(cv);
    }

    private TextView chip(String status) {
        TextView tv = new TextView(requireContext());
        tv.setText(" " + status + " ");
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextSize(11f);
        tv.setSingleLine(true);
        int text = R.color.appGreen;
        int bg = R.drawable.bg_timeline_chip_normal;
        if ("ABNORMAL".equals(status)) text = R.color.vitalRed;
        if ("ABNORMAL".equals(status)) bg = R.drawable.bg_timeline_chip_abnormal;
        if ("BORDERLINE".equals(status)) {
            text = R.color.vitalOrange;
            bg = R.drawable.bg_timeline_chip_borderline;
        }
        tv.setTextColor(ContextCompat.getColor(requireContext(), text));
        tv.setBackgroundResource(bg);
        tv.setPadding(dp(14), dp(7), dp(14), dp(7));
        return tv;
    }

    private void Space(LinearLayout row, int size) {
        View space = new View(requireContext());
        row.addView(space, new LinearLayout.LayoutParams(size, 1));
    }

    private String formatResult(LabResult item) {
        String unit = cleanDisplay(item.unit, "");
        String raw = String.valueOf(item.value);
        if (raw.endsWith(".0")) {
            raw = raw.substring(0, raw.length() - 2);
        }
        return unit.isEmpty() ? raw : raw + " " + unit;
    }

    private String cleanDisplay(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) return fallback;
        return trimmed;
    }

    private int colorForStatus(String status) {
        String s = safe(status);
        if (s.contains("abnormal") || s.contains("high") || s.contains("low")) return R.color.vitalRed;
        if (s.contains("borderline")) return R.color.vitalOrange;
        return R.color.appGreen;
    }

    private int dotForStatus(String status) {
        String s = safe(status);
        if (s.contains("abnormal") || s.contains("high") || s.contains("low")) return R.drawable.circle_bg_purple_light;
        if (s.contains("borderline")) return R.drawable.circle_bg_blue_light;
        return R.drawable.ic_status_dot_green;
    }

    private int organIconRes(String organ) {
        if ("LIVER".equals(organ)) return R.drawable.ic_timeline_liver;
        if ("PANCREAS".equals(organ)) return R.drawable.ic_timeline_pancreas;
        if ("KIDNEYS".equals(organ)) return R.drawable.ic_timeline_kidney;
        if ("HEART".equals(organ)) return R.drawable.ic_blood_pressure;
        return R.drawable.ic_status_dot_green;
    }

    private int organIconBg(String organ) {
        if ("LIVER".equals(organ)) return R.drawable.bg_timeline_icon_liver;
        return R.drawable.bg_timeline_icon_green;
    }

    private String safe(String v) {
        if (v == null || "null".equalsIgnoreCase(v.trim())) return "";
        return v.toLowerCase(Locale.US);
    }

    private void showReportHistory() {
        new Thread(() -> {
            List<com.medisync.diabo.model.MedicalReport> reports = AppDatabase.getInstance(requireContext()).appDao().getAllReportsSync();
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                if (reports == null || reports.isEmpty()) {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Report History")
                            .setMessage("No uploaded or manual reports found yet.")
                            .setPositiveButton("Close", null)
                            .show();
                    return;
                }
                String[] titles = new String[reports.size()];
                for (int i = 0; i < reports.size(); i++) {
                    com.medisync.diabo.model.MedicalReport report = reports.get(i);
                    titles[i] = cleanDisplay(report.title, "Health Report") + " - " + dateFormat.format(report.getDisplayDate());
                }
                new AlertDialog.Builder(requireContext())
                        .setTitle("Report History")
                        .setItems(titles, (dialog, which) -> showReportDetail(reports.get(which)))
                        .setPositiveButton("Close", null)
                        .show();
            });
        }).start();
    }

    private void showReportDetail(com.medisync.diabo.model.MedicalReport report) {
        String detail = "Date: " + dateFormat.format(report.getDisplayDate())
                + "\nType: " + cleanDisplay(report.reportType, "Not specified")
                + "\n\nInsights:\n" + cleanDisplay(report.aiInsights, "No AI insight saved.")
                + "\n\nExtracted text:\n" + cleanDisplay(report.extractedText, "No extracted text saved.");
        new AlertDialog.Builder(requireContext())
                .setTitle(cleanDisplay(report.title, "Report Detail"))
                .setMessage(detail)
                .setPositiveButton("Close", null)
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
