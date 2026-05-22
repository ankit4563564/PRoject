package com.medisync.diabo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.medisync.diabo.databinding.ActivityAddMedicationBinding;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.Medication;
import com.medisync.diabo.service.MedicationReminderReceiver;
import java.util.Calendar;

public class AddMedicationActivity extends AppCompatActivity {

    private ActivityAddMedicationBinding binding;
    private AppDatabase db;
    private Integer reminderHour;
    private Integer reminderMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddMedicationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        db = AppDatabase.getInstance(this);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        binding.tvCancel.setOnClickListener(v -> finish());
        binding.tvSave.setOnClickListener(v -> saveMedication());
        binding.etReminderTime.setOnClickListener(v -> showTimePicker());
    }

    private void showTimePicker() {
        Calendar now = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            reminderHour = hourOfDay;
            reminderMinute = minute;
            binding.etReminderTime.setText(String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute));
        }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);
        dialog.show();
    }

    private void saveMedication() {
        String name = binding.etMedicationName.getText() == null ? "" : binding.etMedicationName.getText().toString().trim();
        String dosage = binding.etDosage.getText() == null ? "" : binding.etDosage.getText().toString().trim();
        String frequency = binding.etFrequency.getText() == null ? "" : binding.etFrequency.getText().toString().trim();
        String instructions = binding.etInstructions.getText() == null ? "" : binding.etInstructions.getText().toString().trim();

        try {
            com.medisync.diabo.service.Validator.validateMedication(name, dosage, frequency);
        } catch (com.medisync.diabo.service.Validator.ValidationException e) {
            com.medisync.diabo.service.NotificationHelper.showError(binding.getRoot(), e.getMessage());
            return;
        }

        binding.tvSave.setEnabled(false);
        binding.pbLoading.setVisibility(android.view.View.VISIBLE);

        Medication medication = new Medication();
        medication.name = name;
        medication.dosage = dosage;
        medication.frequency = frequency;
        medication.instructions = instructions;
        medication.isActive = true;
        medication.source = "Manual Entry";

        new Thread(() -> {
            try {
                db.appDao().insertMedication(medication);
                if (reminderHour != null && reminderMinute != null) {
                    scheduleReminder(medication);
                }
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.pbLoading.setVisibility(android.view.View.GONE);
                        binding.tvSave.setEnabled(true);
                        com.medisync.diabo.service.NotificationHelper.showSuccess(binding.getRoot(), "Medication saved successfully");
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (!isFinishing() && !isDestroyed()) {
                                finish();
                            }
                        }, 1000);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        binding.pbLoading.setVisibility(android.view.View.GONE);
                        binding.tvSave.setEnabled(true);
                        com.medisync.diabo.service.NotificationHelper.showError(binding.getRoot(), "Failed to save medication: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void scheduleReminder(Medication medication) {
        Calendar trigger = Calendar.getInstance();
        trigger.set(Calendar.HOUR_OF_DAY, reminderHour);
        trigger.set(Calendar.MINUTE, reminderMinute);
        trigger.set(Calendar.SECOND, 0);
        trigger.set(Calendar.MILLISECOND, 0);
        if (trigger.before(Calendar.getInstance())) {
            trigger.add(Calendar.DAY_OF_YEAR, 1);
        }

        Intent intent = new Intent(this, MedicationReminderReceiver.class);
        intent.putExtra("name", medication.name);
        intent.putExtra("dosage", medication.dosage);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                medication.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    trigger.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
            );
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
