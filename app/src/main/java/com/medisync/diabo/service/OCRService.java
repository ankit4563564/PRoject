package com.medisync.diabo.service;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;


public class OCRService {
    /**
     * Backwards-compatible wrapper for older code expecting a recognizeText method.
     * Delegates to {@link #extractText(Context, Uri, OCRCallback)}.
     */
    public static void recognizeText(Context context, Uri imageUri, OCRCallback callback) {
        extractText(context, imageUri, callback);
    }

    public interface OCRCallback {
        void onSuccess(String text);
        void onFailure(Exception e);
    }

    public static void extractText(Context context, Uri imageUri, OCRCallback callback) {
        // Dummy OCR Extraction
        // Simulating a delay for processing
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            String dummyText = "LABORATORY REPORT\n" +
                    "Patient Name: John Doe\n" +
                    "Date: 2023-10-27\n\n" +
                    "TEST RESULTS:\n" +
                    "HbA1c: 6.8 %\n" +
                    "Fasting Glucose: 112 mg/dL\n" +
                    "Post-Meal Glucose: 145 mg/dL\n" +
                    "Total Cholesterol: 195 mg/dL\n" +
                    "LDL Cholesterol: 128 mg/dL\n\n" +
                    "MEDICATIONS:\n" +
                    "Metformin 500mg - Twice daily\n" +
                    "Atorvastatin 10mg - Once daily at night";
            
            callback.onSuccess(dummyText);
        }, 2000);
    }
}
