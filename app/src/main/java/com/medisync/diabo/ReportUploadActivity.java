package com.medisync.diabo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.medisync.diabo.databinding.ActivityReportUploadBinding;
import com.medisync.diabo.service.OllamaRouter;
import com.medisync.diabo.service.ReportService;
import com.medisync.diabo.model.MedicalReport;

public class ReportUploadActivity extends AppCompatActivity {

    private ActivityReportUploadBinding binding;
    private final Handler statusHandler = new Handler(Looper.getMainLooper());
    private int statusTick = 0;
    private final Runnable statusPulse = new Runnable() {
        @Override
        public void run() {
            String[] lines = new String[]{
                    "Analyzing with AI...",
                    "Processing image 1 of 1",
                    "Structuring parameters...",
                    "Finalizing report..."
            };
            binding.tvStatus.setText(lines[Math.min(statusTick, lines.length - 1)]);
            statusTick++;
            if (statusTick < lines.length) {
                statusHandler.postDelayed(this, 520);
            }
        }
    };
    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleSelectedImage
    );
    private final ActivityResultLauncher<String> documentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleSelectedImage
    );

    private boolean isGuestUser() {
        return getSharedPreferences("diabo_prefs", MODE_PRIVATE).getBoolean("is_guest", false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnDone.setOnClickListener(v -> finish());
        binding.btnCheckBackend.setOnClickListener(v -> checkBackendStatus());

        binding.btnPickImage.setOnClickListener(v -> {
            if (isGuestUser()) {
                new com.medisync.diabo.fragment.GuestLimitDialog().show(getSupportFragmentManager(), "GuestLimitDialog");
            } else {
                galleryLauncher.launch("image/*");
            }
        });
        binding.btnTakePhoto.setOnClickListener(v -> {
            if (isGuestUser()) {
                new com.medisync.diabo.fragment.GuestLimitDialog().show(getSupportFragmentManager(), "GuestLimitDialog");
            } else {
                documentLauncher.launch("application/pdf");
            }
        });
    }

    private void checkBackendStatus() {
        binding.btnCheckBackend.setEnabled(false);
        binding.pbBackendLoading.setVisibility(View.VISIBLE);
        binding.tvBackendStatus.setText("Checking...");
        binding.tvBackendStatus.setTextColor(ContextCompat.getColor(this, R.color.graySubtext));
        
        OllamaRouter.ping(this, BuildConfig.OLLAMA_MODEL, new OllamaRouter.TextCallback() {
            @Override
            public void onSuccess(String text) {
                runOnUiThread(() -> {
                    binding.btnCheckBackend.setEnabled(true);
                    binding.pbBackendLoading.setVisibility(View.GONE);
                    binding.tvBackendStatus.setText(text);
                    binding.tvBackendStatus.setTextColor(ContextCompat.getColor(ReportUploadActivity.this, R.color.appGreen));
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    binding.btnCheckBackend.setEnabled(true);
                    binding.pbBackendLoading.setVisibility(View.GONE);
                    binding.tvBackendStatus.setText("Connection failed: " + error);
                    binding.tvBackendStatus.setTextColor(ContextCompat.getColor(ReportUploadActivity.this, R.color.vitalRed));
                });
            }
        });
    }

    private void handleSelectedImage(Uri uri) {
        if (uri == null) return;

        try {
            String mimeType = getContentResolver().getType(uri);
            if (mimeType != null && mimeType.startsWith("image/")) {
                binding.ivPreview.setImageURI(uri);
            } else {
                binding.ivPreview.setImageResource(R.drawable.ic_attachment);
            }
            binding.ivPreview.setVisibility(View.VISIBLE);
            binding.btnPickImage.setVisibility(View.GONE);
            binding.btnTakePhoto.setVisibility(View.GONE);

            binding.progressBar.setVisibility(View.VISIBLE);
            binding.tvStatus.setVisibility(View.VISIBLE);
            statusTick = 0;
            statusHandler.removeCallbacksAndMessages(null);
            statusHandler.post(statusPulse);
            binding.analysisCard.animate().alpha(1f).setDuration(180).start();

            ReportService.processReport(this, uri, new ReportService.ReportCallback() {
                @Override
                public void onSuccess(MedicalReport report) {
                    runOnUiThread(() -> {
                        statusHandler.removeCallbacksAndMessages(null);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvStatus.setText("PDF Uploaded Successfully");
                        Toast.makeText(ReportUploadActivity.this, "Analysis Successful!", Toast.LENGTH_LONG).show();
                        statusHandler.postDelayed(ReportUploadActivity.this::finish, 420);
                    });
                }

                @Override
                public void onFailure(String error) {
                    runOnUiThread(() -> {
                        statusHandler.removeCallbacksAndMessages(null);
                        binding.progressBar.setVisibility(View.GONE);
                        binding.tvStatus.setText("Error: " + error);
                        binding.btnPickImage.setVisibility(View.VISIBLE);
                        binding.btnTakePhoto.setVisibility(View.VISIBLE);
                        Toast.makeText(ReportUploadActivity.this, error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            statusHandler.removeCallbacksAndMessages(null);
            binding.progressBar.setVisibility(View.GONE);
            binding.tvStatus.setText("Error reading file: " + e.getMessage());
            binding.btnPickImage.setVisibility(View.VISIBLE);
            binding.btnTakePhoto.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Failed to read document: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        statusHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
