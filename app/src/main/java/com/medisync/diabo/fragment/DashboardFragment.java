package com.medisync.diabo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import com.medisync.diabo.R;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.databinding.FragmentDashboardBinding;
import com.medisync.diabo.AddMedicationActivity;
import com.medisync.diabo.model.LabResult;
import com.medisync.diabo.model.Medication;
import com.medisync.diabo.model.UserProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private AppDatabase db;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private UserProfile currentProfile;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        db = AppDatabase.getInstance(requireContext());

        binding.ivProfile.setOnClickListener(v -> showProfileDialog());
        binding.btnSearch.setOnClickListener(v -> showSearchDialog());
        binding.btnLogGlucose.setOnClickListener(v -> showGlucoseLogDialog());
        binding.btnChatHistory.setOnClickListener(v -> showChatHistoryDialog());

        binding.tvSeeLogs.setOnClickListener(v -> {
            if (getParentFragment() instanceof MainFragment) {
                ((MainFragment) getParentFragment()).selectTab(R.id.nav_timeline);
            } else {
                android.widget.Toast.makeText(requireContext(), "Opening health logs...", android.widget.Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAddMedication.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), AddMedicationActivity.class));
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        binding.btnAddMedication.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(70).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
            }
            return false;
        });

        db.appDao().getActiveMedications().observe(getViewLifecycleOwner(), this::bindActiveMedication);
        db.appDao().getUserProfile().observe(getViewLifecycleOwner(), this::bindUserProfile);
        db.appDao().getAllLabResults().observe(getViewLifecycleOwner(), this::bindLabResults);
    }

    private void bindUserProfile(com.medisync.diabo.model.UserProfile profile) {
        if (profile == null) return;
        currentProfile = profile;
        
        binding.tvGreeting.setText("Hello, " + (profile.name != null ? profile.name : "User"));
        binding.tvDiabetesBadge.setText(profile.diabetesType != null ? profile.diabetesType : "Type 2");
        binding.tvUserSubtitle.setText(profile.treatmentType != null ? profile.treatmentType : "Oral Medications");
        
        if (profile.weight != null && profile.weight > 0) {
            binding.tvWeightValue.setText(String.format(java.util.Locale.US, "%.1f", profile.weight));
            binding.tvWeightStatus.setText("Profile Vitals");
            binding.tvWeightStatus.setTextColor(requireContext().getColor(R.color.appGreen));
        } else {
            binding.tvWeightValue.setText("--");
            binding.tvWeightStatus.setText("No data");
            binding.tvWeightStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
        }
        
        if (profile.height != null && profile.height > 0 && profile.weight != null && profile.weight > 0) {
            double heightM = profile.height / 100.0;
            double bmi = profile.weight / (heightM * heightM);
            binding.tvBmiValue.setText(String.format(java.util.Locale.US, "%.1f", bmi));
            
            String bmiStatus;
            int color;
            if (bmi < 18.5) {
                bmiStatus = "Underweight";
                color = requireContext().getColor(R.color.appOrange);
            } else if (bmi < 25) {
                bmiStatus = "Normal";
                color = requireContext().getColor(R.color.appGreen);
            } else if (bmi < 30) {
                bmiStatus = "Overweight";
                color = requireContext().getColor(R.color.appOrange);
            } else {
                bmiStatus = "Obese";
                color = requireContext().getColor(R.color.vitalRed);
            }
            binding.tvBmiStatus.setText(bmiStatus);
            binding.tvBmiStatus.setTextColor(color);
        } else {
            binding.tvBmiValue.setText("--");
            binding.tvBmiStatus.setText("No data");
            binding.tvBmiStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
        }
    }

    private void showProfileDialog() {
        UserProfile profile = currentProfile;
        String message;
        if (profile == null) {
            message = "No profile details saved yet.";
        } else {
            message = "Name: " + clean(profile.name, "User")
                    + "\nAge: " + profile.age
                    + "\nGender: " + clean(profile.gender, "Not set")
                    + "\nDiabetes: " + clean(profile.diabetesType, "Not set")
                    + "\nTreatment: " + clean(profile.treatmentType, "Not set")
                    + "\nHeight: " + (profile.height == null ? "Not set" : profile.height + " cm")
                    + "\nWeight: " + (profile.weight == null ? "Not set" : profile.weight + " kg");
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("My Profile")
                .setMessage(message)
                .setPositiveButton("Edit", (dialog, which) -> showEditProfileDialog())
                .setNegativeButton("Logout", (dialog, which) -> logout())
                .setNeutralButton("Close", null)
                .show();
    }

    private void showEditProfileDialog() {
        if (currentProfile == null) {
            android.widget.Toast.makeText(requireContext(), "Create your profile first.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), 0);

        EditText name = field("Full name", currentProfile.name, InputType.TYPE_CLASS_TEXT);
        EditText age = field("Age", String.valueOf(currentProfile.age), InputType.TYPE_CLASS_NUMBER);
        EditText diabetes = field("Diabetes type", currentProfile.diabetesType, InputType.TYPE_CLASS_TEXT);
        EditText treatment = field("Treatment", currentProfile.treatmentType, InputType.TYPE_CLASS_TEXT);
        EditText height = field("Height cm", currentProfile.height == null ? "" : String.valueOf(currentProfile.height), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText weight = field("Weight kg", currentProfile.weight == null ? "" : String.valueOf(currentProfile.weight), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(name);
        root.addView(age);
        root.addView(diabetes);
        root.addView(treatment);
        root.addView(height);
        root.addView(weight);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    currentProfile.name = name.getText().toString().trim();
                    currentProfile.age = parseInt(age.getText().toString(), currentProfile.age);
                    currentProfile.diabetesType = diabetes.getText().toString().trim();
                    currentProfile.treatmentType = treatment.getText().toString().trim();
                    currentProfile.height = parseDoubleOrNull(height.getText().toString());
                    currentProfile.weight = parseDoubleOrNull(weight.getText().toString());
                    executor.execute(() -> db.appDao().updateUserProfile(currentProfile));
                })
                .show();
    }

    private void logout() {
        executor.execute(() -> {
            db.appDao().clearUserProfiles();
            requireContext().getSharedPreferences("diabo_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.mainFragment, true)
                        .build();
                try {
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.authenticationFragment, null, options);
                } catch (Exception ignored) {
                }
            });
        });
    }

    private void showSearchDialog() {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, dp(8), pad, 0);

        EditText input = new EditText(requireContext());
        input.setHint("Search labs, medications, reports");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        root.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView results = new TextView(requireContext());
        results.setTextColor(requireContext().getColor(R.color.grayText));
        results.setTextSize(14f);
        results.setPadding(0, dp(14), 0, 0);

        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Search Health Data")
                .setView(root)
                .setPositiveButton("Close", null)
                .create();

        input.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                runSearch(s.toString(), results);
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        dialog.setOnShowListener(d -> runSearch("", results));
        dialog.show();
    }

    private void showGlucoseLogDialog() {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), 0);
        EditText value = field("Glucose value mg/dL", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText type = field("Type: Fasting or Post-Meal", "Fasting", InputType.TYPE_CLASS_TEXT);
        root.addView(value);
        root.addView(type);

        new AlertDialog.Builder(requireContext())
                .setTitle("Manual Glucose Entry")
                .setView(root)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> saveGlucoseLog(value.getText().toString(), type.getText().toString()))
                .show();
    }

    private void saveGlucoseLog(String valueText, String typeText) {
        double value;
        try {
            value = Double.parseDouble(valueText.trim());
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "Enter a valid glucose value.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        String type = typeText == null || typeText.trim().isEmpty() ? "Glucose" : typeText.trim();
        executor.execute(() -> {
            com.medisync.diabo.model.MedicalReport report = new com.medisync.diabo.model.MedicalReport();
            report.title = "Manual Glucose Entry";
            report.reportType = "Manual Glucose";
            report.extractedText = type + ": " + value + " mg/dL";
            db.appDao().insertReport(report);

            LabResult result = new LabResult();
            result.testName = type.toLowerCase(Locale.US).contains("post") ? "Post-Meal Glucose" : "Fasting Glucose";
            result.parameter = result.testName;
            result.value = value;
            result.unit = "mg/dL";
            result.category = "Glucose";
            result.reportId = report.id;
            result.status = value > (result.testName.startsWith("Post") ? 140 : 125) ? "High" : value < 70 ? "Low" : "Normal";
            db.appDao().insertLabResults(java.util.Collections.singletonList(result));
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) android.widget.Toast.makeText(requireContext(), "Glucose log saved.", android.widget.Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void showChatHistoryDialog() {
        String history = requireContext()
                .getSharedPreferences("chat_history", android.content.Context.MODE_PRIVATE)
                .getString("messages", "");
        new AlertDialog.Builder(requireContext())
                .setTitle("Chat History")
                .setMessage(history == null || history.trim().isEmpty() ? "No saved chat history yet." : history)
                .setPositiveButton("Open Chat", (dialog, which) -> {
                    if (getParentFragment() instanceof MainFragment) {
                        ((MainFragment) getParentFragment()).selectTab(R.id.nav_chat);
                    }
                })
                .setNegativeButton("Clear", (dialog, which) -> requireContext().getSharedPreferences("chat_history", android.content.Context.MODE_PRIVATE).edit().clear().apply())
                .setNeutralButton("Close", null)
                .show();
    }

    private EditText field(String hint, String value, int inputType) {
        EditText editText = new EditText(requireContext());
        editText.setHint(hint);
        editText.setText(value == null ? "" : value);
        editText.setInputType(inputType);
        editText.setSingleLine(true);
        editText.setPadding(0, dp(8), 0, dp(8));
        return editText;
    }

    private void runSearch(String query, TextView resultsView) {
        executor.execute(() -> {
            String q = query == null ? "" : query.trim().toLowerCase(Locale.US);
            List<String> lines = new ArrayList<>();
            for (LabResult lab : db.appDao().getAllLabResultsSync()) {
                String text = clean(lab.testName, "Lab result") + " " + lab.value + " " + clean(lab.unit, "") + " " + clean(lab.status, "");
                if (q.isEmpty() || text.toLowerCase(Locale.US).contains(q)) {
                    lines.add("Lab: " + text);
                }
            }
            for (Medication med : db.appDao().getActiveMedicationsSync()) {
                String text = clean(med.name, "Medication") + " " + clean(med.dosage, "") + " " + clean(med.frequency, "");
                if (q.isEmpty() || text.toLowerCase(Locale.US).contains(q)) {
                    lines.add("Medication: " + text);
                }
            }
            String output = lines.isEmpty() ? "No matching health data found." : android.text.TextUtils.join("\n\n", lines);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (isAdded()) resultsView.setText(output);
            });
        });
    }

    private String clean(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) return fallback;
        return value.trim();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private Double parseDoubleOrNull(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return null;
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void bindLabResults(List<com.medisync.diabo.model.LabResult> results) {
        if (results == null || results.isEmpty()) {
            binding.tvHba1cValue.setText("No data yet");
            binding.tvFastingValue.setText("--");
            binding.tvFastingStatus.setText("No data");
            binding.tvFastingStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
            binding.tvPostmealValue.setText("--");
            binding.tvPostmealStatus.setText("No data");
            binding.tvPostmealStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
            binding.tvBpValue.setText("--/--");
            binding.tvBpStatus.setText("No data");
            binding.tvBpStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
            return;
        }

        com.medisync.diabo.model.LabResult latestHbA1c = null;
        com.medisync.diabo.model.LabResult latestFasting = null;
        com.medisync.diabo.model.LabResult latestPostMeal = null;
        com.medisync.diabo.model.LabResult latestBp = null;

        for (com.medisync.diabo.model.LabResult result : results) {
            if (result.testName == null && result.parameter == null && result.category == null) continue;
            String testName = result.testName != null ? result.testName.toLowerCase() : "";
            String param = result.parameter != null ? result.parameter.toLowerCase() : "";
            String cat = result.category != null ? result.category.toLowerCase() : "";

            if (latestHbA1c == null && (testName.contains("hba1c") || param.contains("hba1c") || cat.contains("hba1c") || testName.contains("a1c") || param.contains("a1c"))) {
                latestHbA1c = result;
            }
            if (latestFasting == null && (testName.contains("fasting") || param.contains("fasting"))) {
                latestFasting = result;
            }
            if (latestPostMeal == null && (testName.contains("post-meal") || param.contains("post-meal") || testName.contains("post meal") || param.contains("post meal") || testName.contains("postmeal") || param.contains("postmeal"))) {
                latestPostMeal = result;
            }
            if (latestBp == null && (testName.contains("blood pressure") || param.contains("blood pressure") || cat.equals("bp") || testName.equals("bp") || testName.contains("pressure"))) {
                latestBp = result;
            }
        }

        if (latestHbA1c != null) {
            String valStr = latestHbA1c.stringValue != null && !latestHbA1c.stringValue.isEmpty() ? latestHbA1c.stringValue : String.format(java.util.Locale.US, "%.1f%%", latestHbA1c.value);
            if (!valStr.contains("%")) {
                valStr += "%";
            }
            binding.tvHba1cValue.setText(valStr);
        } else {
            binding.tvHba1cValue.setText("No data yet");
        }

        if (latestFasting != null) {
            binding.tvFastingValue.setText(String.format(java.util.Locale.US, "%.0f", latestFasting.value));
            binding.tvFastingStatus.setText(latestFasting.status != null ? latestFasting.status : "Normal");
            int color = "High".equalsIgnoreCase(latestFasting.status) ? requireContext().getColor(R.color.vitalRed) : ("Low".equalsIgnoreCase(latestFasting.status) ? requireContext().getColor(R.color.appOrange) : requireContext().getColor(R.color.appGreen));
            binding.tvFastingStatus.setTextColor(color);
        } else {
            binding.tvFastingValue.setText("--");
            binding.tvFastingStatus.setText("No data");
            binding.tvFastingStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
        }

        if (latestPostMeal != null) {
            binding.tvPostmealValue.setText(String.format(java.util.Locale.US, "%.0f", latestPostMeal.value));
            binding.tvPostmealStatus.setText(latestPostMeal.status != null ? latestPostMeal.status : "Normal");
            int color = "High".equalsIgnoreCase(latestPostMeal.status) ? requireContext().getColor(R.color.vitalRed) : ("Low".equalsIgnoreCase(latestPostMeal.status) ? requireContext().getColor(R.color.appOrange) : requireContext().getColor(R.color.appGreen));
            binding.tvPostmealStatus.setTextColor(color);
        } else {
            binding.tvPostmealValue.setText("--");
            binding.tvPostmealStatus.setText("No data");
            binding.tvPostmealStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
        }

        if (latestBp != null) {
            binding.tvBpValue.setText(latestBp.stringValue != null && !latestBp.stringValue.isEmpty() ? latestBp.stringValue : String.format(java.util.Locale.US, "%.0f", latestBp.value));
            binding.tvBpStatus.setText(latestBp.status != null ? latestBp.status : "Normal");
            int color = "High".equalsIgnoreCase(latestBp.status) ? requireContext().getColor(R.color.vitalRed) : requireContext().getColor(R.color.appGreen);
            binding.tvBpStatus.setTextColor(color);
        } else {
            binding.tvBpValue.setText("--/--");
            binding.tvBpStatus.setText("No data");
            binding.tvBpStatus.setTextColor(requireContext().getColor(R.color.graySubtext));
        }
    }

    private void bindActiveMedication(List<Medication> medications) {
        if (medications == null || medications.isEmpty()) {
            binding.cardEmptyActiveMedications.setVisibility(View.VISIBLE);
            binding.cardActiveMedication.setVisibility(View.GONE);
            return;
        }
        Medication med = medications.get(0);
        binding.cardEmptyActiveMedications.setVisibility(View.GONE);
        binding.cardActiveMedication.setVisibility(View.VISIBLE);
        binding.tvActiveMedName.setText(med.name == null ? "Medication" : med.name);
        binding.tvActiveMedDosage.setText((med.dosage == null ? "--" : med.dosage) + " - " + (med.frequency == null ? "--" : med.frequency));
        binding.tvActiveMedFrequency.setText(med.instructions == null || med.instructions.isEmpty() ? "Manual entry" : med.instructions);
        binding.cardActiveMedication.setAlpha(0f);
        binding.cardActiveMedication.setTranslationY(18f);
        binding.cardActiveMedication.animate().alpha(1f).translationY(0f).setDuration(220).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}

