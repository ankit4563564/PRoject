package com.medisync.diabo.fragment;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.medisync.diabo.R;
import com.medisync.diabo.databinding.FragmentSetupBinding;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.UserProfile;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class SetupFragment extends Fragment {

    private FragmentSetupBinding binding;
    private AppDatabase db;
    private int currentStep = 1;
    private String selectedDiabetesType = "Type 2";
    private String selectedTreatment = "Oral Medications";
    private int lastRenderedStep = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSetupBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getInstance(requireContext());
        setupSelectionListeners();
        setupVitalsListeners();
        renderStep();

        binding.btnCompleteSetup.setOnClickListener(v -> {
            if (currentStep < 5) {
                currentStep++;
                renderStep();
                return;
            }
            if (!binding.cbAgree.isChecked()) {
                Toast.makeText(requireContext(), "Please accept terms to continue", Toast.LENGTH_SHORT).show();
                return;
            }
            saveUserProfile();
        });

        binding.btnBack.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                renderStep();
                return;
            }
            requireActivity().onBackPressed();
        });
        binding.cbAgree.setOnCheckedChangeListener((buttonView, isChecked) -> updateContinueState());
    }

    private void setupSelectionListeners() {
        binding.optionType1.setOnClickListener(v -> selectDiabetesType("Type 1", binding.optionType1));
        binding.optionType2.setOnClickListener(v -> selectDiabetesType("Type 2", binding.optionType2));
        binding.optionPrediabetes.setOnClickListener(v -> selectDiabetesType("Prediabetes", binding.optionPrediabetes));
        binding.optionGestational.setOnClickListener(v -> selectDiabetesType("Gestational", binding.optionGestational));

        binding.optionTreatInsulin.setOnClickListener(v -> selectTreatment("Insulin Therapy", binding.optionTreatInsulin));
        binding.optionTreatOral.setOnClickListener(v -> selectTreatment("Oral Medications", binding.optionTreatOral));
        binding.optionTreatInjectable.setOnClickListener(v -> selectTreatment("Injectable (GLP-1)", binding.optionTreatInjectable));
        binding.optionTreatLifestyle.setOnClickListener(v -> selectTreatment("Lifestyle Only", binding.optionTreatLifestyle));
    }

    private void setupVitalsListeners() {
        TextWatcher bmiWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateBmiPreview();
            }
        };
        binding.etHeight.addTextChangedListener(bmiWatcher);
        binding.etWeight.addTextChangedListener(bmiWatcher);
    }

    private void selectDiabetesType(String type, MaterialCardView selectedCard) {
        selectedDiabetesType = type;
        clearTypeSelection();
        selectedCard.setStrokeColor(requireContext().getColor(R.color.vibrantPurple));
        selectedCard.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
    }

    private void selectTreatment(String treatment, MaterialCardView selectedCard) {
        selectedTreatment = treatment;
        clearTreatmentSelection();
        selectedCard.setStrokeColor(requireContext().getColor(R.color.vibrantPurple));
        selectedCard.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
    }

    private void clearTypeSelection() {
        int stroke = (int) getResources().getDisplayMetrics().density;
        int divider = requireContext().getColor(R.color.divider);
        binding.optionType1.setStrokeColor(divider);
        binding.optionType2.setStrokeColor(divider);
        binding.optionPrediabetes.setStrokeColor(divider);
        binding.optionGestational.setStrokeColor(divider);
        binding.optionType1.setStrokeWidth(stroke);
        binding.optionType2.setStrokeWidth(stroke);
        binding.optionPrediabetes.setStrokeWidth(stroke);
        binding.optionGestational.setStrokeWidth(stroke);
    }

    private void clearTreatmentSelection() {
        int stroke = (int) getResources().getDisplayMetrics().density;
        int divider = requireContext().getColor(R.color.divider);
        binding.optionTreatInsulin.setStrokeColor(divider);
        binding.optionTreatOral.setStrokeColor(divider);
        binding.optionTreatInjectable.setStrokeColor(divider);
        binding.optionTreatLifestyle.setStrokeColor(divider);
        binding.optionTreatInsulin.setStrokeWidth(stroke);
        binding.optionTreatOral.setStrokeWidth(stroke);
        binding.optionTreatInjectable.setStrokeWidth(stroke);
        binding.optionTreatLifestyle.setStrokeWidth(stroke);
    }

    private void renderStep() {
        View previousContainer = getContainerForStep(lastRenderedStep);
        View activeContainer = getContainerForStep(currentStep);
        for (int step = 1; step <= 5; step++) {
            getContainerForStep(step).setVisibility(View.GONE);
        }
        if (activeContainer != null) {
            activeContainer.setVisibility(View.VISIBLE);
        }
        animateStepContainer(activeContainer, currentStep >= lastRenderedStep);
        animateProgress(lastRenderedStep, currentStep);

        binding.tvProgressNum.setText(String.valueOf(currentStep));
        binding.tvStepIndex.setText("Step " + currentStep + " of 5");
        binding.btnCompleteSetup.setText(currentStep == 5 ? "Get Started" : "Continue");

        if (currentStep == 1) {
            binding.tvStepTitle.setText("Diabetes Type");
            binding.tvQuestion.setText("What type of diabetes do you have?");
            binding.tvSubtitle.setText("This helps us personalize your glucose targets and alerts.");
        } else if (currentStep == 2) {
            binding.tvStepTitle.setText("Treatment Plan");
            binding.tvQuestion.setText("How are you managing your diabetes?");
            binding.tvSubtitle.setText("Your treatment type affects our safety alerts and medication tracking.");
        } else if (currentStep == 3) {
            binding.tvStepTitle.setText("Risk Factors");
            binding.tvQuestion.setText("Any related health conditions?");
            binding.tvSubtitle.setText("This helps us assess complication risks and customize monitoring.");
        } else if (currentStep == 4) {
            binding.tvStepTitle.setText("Personal Info");
            binding.tvQuestion.setText("Tell us about yourself");
            binding.tvSubtitle.setText("Used for personalized targets and BMI calculation.");
            updateBmiPreview();
        } else {
            binding.tvStepTitle.setText("Privacy & Terms");
            binding.tvQuestion.setText("Your privacy matters");
            binding.tvSubtitle.setText("Review permissions and finish setup.");
        }
        binding.setupScroll.fullScroll(View.FOCUS_UP);
        if (previousContainer != null && previousContainer != activeContainer) {
            previousContainer.setVisibility(View.GONE);
        }
        updateContinueState();
        lastRenderedStep = currentStep;
    }

    private View getContainerForStep(int step) {
        if (step == 1) return binding.step1Container;
        if (step == 2) return binding.step2Container;
        if (step == 3) return binding.step3Container;
        if (step == 4) return binding.step4Container;
        return binding.step5Container;
    }

    private void animateStepContainer(View container, boolean forward) {
        if (container == null) return;
        container.setAlpha(0f);
        container.setTranslationX(forward ? 24f : -24f);
        container.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(260)
                .start();
    }

    private void animateProgress(int from, int to) {
        ObjectAnimator animator = ObjectAnimator.ofInt(binding.progressCircle, "progress", from, to);
        animator.setDuration(280);
        animator.setInterpolator(new OvershootInterpolator(0.7f));
        animator.start();
    }

    private void updateContinueState() {
        boolean enabled = currentStep != 5 || binding.cbAgree.isChecked();
        binding.btnCompleteSetup.setEnabled(enabled);
        binding.btnCompleteSetup.animate().alpha(enabled ? 1f : 0.55f).setDuration(140).start();
    }

    private void updateBmiPreview() {
        String heightText = binding.etHeight.getText() == null ? "" : binding.etHeight.getText().toString().trim();
        String weightText = binding.etWeight.getText() == null ? "" : binding.etWeight.getText().toString().trim();
        if (heightText.isEmpty() || weightText.isEmpty()) {
            binding.tvBmiValue.setText("--");
            binding.tvBmiStatus.setText("Enter height and weight");
            return;
        }
        double heightCm = parseDouble(heightText, 0.0);
        double weightKg = parseDouble(weightText, 0.0);
        if (heightCm <= 0) {
            binding.tvBmiValue.setText("--");
            binding.tvBmiStatus.setText("Enter height and weight");
            return;
        }
        double bmi = weightKg / Math.pow(heightCm / 100.0, 2.0);
        binding.tvBmiValue.setText(String.format(java.util.Locale.US, "%.1f", bmi));
        if (bmi < 18.5) {
            binding.tvBmiStatus.setText("Underweight");
        } else if (bmi < 25) {
            binding.tvBmiStatus.setText("Normal");
        } else if (bmi < 30) {
            binding.tvBmiStatus.setText("Overweight");
        } else {
            binding.tvBmiStatus.setText("Obesity");
        }
    }

    private void saveUserProfile() {
        String name = binding.etName.getText() == null ? "" : binding.etName.getText().toString().trim();
        String diagnosisYearStr = binding.etDiagnosisYear.getText() == null ? "" : binding.etDiagnosisYear.getText().toString().trim();
        String heightStr = binding.etHeight.getText() == null ? "" : binding.etHeight.getText().toString().trim();
        String weightStr = binding.etWeight.getText() == null ? "" : binding.etWeight.getText().toString().trim();
        
        try {
            com.medisync.diabo.service.Validator.validateUserProfile(name, "29", heightStr, weightStr, diagnosisYearStr);
        } catch (com.medisync.diabo.service.Validator.ValidationException e) {
            com.medisync.diabo.service.NotificationHelper.showError(binding.getRoot(), e.getMessage());
            return;
        }

        UserProfile profile = new UserProfile();
        profile.name = name;
        profile.age = 29;
        profile.diabetesType = selectedDiabetesType;
        profile.treatmentType = selectedTreatment;
        profile.diagnosisYear = diagnosisYearStr.isEmpty() ? null : Integer.parseInt(diagnosisYearStr);
        profile.height = Double.parseDouble(heightStr);
        profile.weight = Double.parseDouble(weightStr);
        profile.enableAI = binding.swAiInsights.isChecked();
        profile.familyHistory = binding.swFamilyHistory.isChecked() ? "Family history present" : "None";
        profile.comorbidities = readSelectedRisks();
        profile.gender = "Male";

        binding.btnCompleteSetup.setEnabled(false);
        binding.pbLoading.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                db.appDao().insertUserProfile(profile);
                
                String fastingStr = binding.etFasting.getText() == null ? "" : binding.etFasting.getText().toString().trim();
                String postmealStr = binding.etPostmeal.getText() == null ? "" : binding.etPostmeal.getText().toString().trim();
                String systolicStr = binding.etSystolic.getText() == null ? "" : binding.etSystolic.getText().toString().trim();
                String diastolicStr = binding.etDiastolic.getText() == null ? "" : binding.etDiastolic.getText().toString().trim();

                if (!fastingStr.isEmpty() || !postmealStr.isEmpty() || !systolicStr.isEmpty() || !diastolicStr.isEmpty()) {
                    List<com.medisync.diabo.model.LabResult> initialResults = new ArrayList<>();
                    String initialReportId = java.util.UUID.randomUUID().toString();
                    
                    com.medisync.diabo.model.MedicalReport initialReport = new com.medisync.diabo.model.MedicalReport();
                    initialReport.id = initialReportId;
                    initialReport.title = "Initial Profile Vitals";
                    initialReport.reportType = "Initial Vitals";
                    initialReport.extractedText = "Initial manual vitals during profile setup.";
                    db.appDao().insertReport(initialReport);

                    if (!fastingStr.isEmpty()) {
                        com.medisync.diabo.model.LabResult r = new com.medisync.diabo.model.LabResult();
                        r.testName = "Fasting Glucose";
                        r.parameter = "Fasting Glucose";
                        r.value = Double.parseDouble(fastingStr);
                        r.unit = "mg/dL";
                        r.category = "Glucose";
                        r.reportId = initialReportId;
                        r.status = r.value > 125 ? "High" : (r.value < 70 ? "Low" : "Normal");
                        initialResults.add(r);
                    }
                    if (!postmealStr.isEmpty()) {
                        com.medisync.diabo.model.LabResult r = new com.medisync.diabo.model.LabResult();
                        r.testName = "Post-Meal Glucose";
                        r.parameter = "Post-Meal Glucose";
                        r.value = Double.parseDouble(postmealStr);
                        r.unit = "mg/dL";
                        r.category = "Glucose";
                        r.reportId = initialReportId;
                        r.status = r.value > 140 ? "High" : (r.value < 70 ? "Low" : "Normal");
                        initialResults.add(r);
                    }
                    if (!systolicStr.isEmpty() || !diastolicStr.isEmpty()) {
                        com.medisync.diabo.model.LabResult r = new com.medisync.diabo.model.LabResult();
                        r.testName = "Blood Pressure";
                        r.parameter = "Blood Pressure";
                        double sys = systolicStr.isEmpty() ? 120.0 : Double.parseDouble(systolicStr);
                        double dia = diastolicStr.isEmpty() ? 80.0 : Double.parseDouble(diastolicStr);
                        r.stringValue = (int)sys + "/" + (int)dia;
                        r.unit = "mmHg";
                        r.category = "BP";
                        r.reportId = initialReportId;
                        r.status = (sys > 130 || dia > 80) ? "High" : "Normal";
                        initialResults.add(r);
                    }

                    if (!initialResults.isEmpty()) {
                        db.appDao().insertLabResults(initialResults);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    if (isAdded() && binding != null) {
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.btnCompleteSetup.setEnabled(true);
                        com.medisync.diabo.service.NotificationHelper.showSuccess(binding.getRoot(), "Profile saved successfully!");
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isAdded() && binding != null) {
                                try {
                                    androidx.navigation.Navigation.findNavController(binding.getRoot()).navigate(R.id.action_setup_to_welcome);
                                } catch (Exception ignored) {
                                }
                            }
                        }, 1000);
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded() && binding != null) {
                        binding.pbLoading.setVisibility(View.GONE);
                        binding.btnCompleteSetup.setEnabled(true);
                        com.medisync.diabo.service.NotificationHelper.showError(binding.getRoot(), "Failed to save profile: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private String readSelectedRisks() {
        List<String> risks = new ArrayList<>();
        int[] chipIds = new int[]{
                R.id.chip_risk_hypertension,
                R.id.chip_risk_dyslipidemia,
                R.id.chip_risk_neuropathy,
                R.id.chip_risk_retinopathy,
                R.id.chip_risk_kidney,
                R.id.chip_risk_obesity
        };
        for (int chipId : chipIds) {
            Chip chip = binding.getRoot().findViewById(chipId);
            if (chip != null && chip.isChecked()) {
                risks.add(chip.getText().toString());
            }
        }
        return String.join(", ", risks);
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception ignore) {
            return fallback;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
