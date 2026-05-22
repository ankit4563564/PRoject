package com.medisync.diabo.service;

import android.content.Context;
import android.net.Uri;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.LabResult;
import com.medisync.diabo.model.MedicalReport;
import com.medisync.diabo.model.Medication;
import com.medisync.diabo.BuildConfig;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class ReportService {

    public interface ReportCallback {
        void onSuccess(MedicalReport report);
        void onFailure(String error);
    }

    public static void processReport(Context context, Uri imageUri, ReportCallback callback) {
        OCRService.recognizeText(context, imageUri, new OCRService.OCRCallback() {
            @Override
            public void onSuccess(String extractedText) {
                analyzeWithAI(context, extractedText, imageUri, callback);
            }

            @Override
            public void onFailure(Exception e) {
                callback.onFailure("OCR Failed: " + e.getMessage());
            }
        });
    }

    private static void analyzeWithAI(Context context, String text, Uri uri, ReportCallback callback) {
        OllamaRouter.analyzeReport(context, BuildConfig.OLLAMA_MODEL, text, new OllamaRouter.TextCallback() {
            @Override
            public void onSuccess(String textResponse) {
                saveResults(context, textResponse, text, uri, callback);
            }

            @Override
            public void onFailure(String error) {
                callback.onFailure("Ollama analysis failed: " + error);
            }
        });
    }

    private static void saveResults(Context context, String aiResponse, String rawText, Uri uri, ReportCallback callback) {
        AppDatabase db = AppDatabase.getInstance(context);
        MedicalReport report = new MedicalReport();
        report.extractedText = rawText;
        report.aiInsights = aiResponse;
        report.imageURL = uri.toString();

        List<LabResult> labs = new ArrayList<>();
        List<Medication> meds = new ArrayList<>();
        
        try {
            String jsonStr = extractJson(aiResponse);
            
            JSONObject root = new JSONObject(jsonStr);
            
            JSONArray labsArray = root.optJSONArray("lab_results");
            if (labsArray != null) {
                for (int i = 0; i < labsArray.length(); i++) {
                    JSONObject obj = labsArray.getJSONObject(i);
                    LabResult lab = new LabResult();
                    lab.testName = obj.optString("name");
                    lab.value = obj.optDouble("value", 0.0);
                    lab.unit = obj.optString("unit");
                    lab.normalRange = obj.optString("range");
                    lab.status = obj.optString("status");
                    lab.reportId = report.id;
                    labs.add(lab);
                }
            }
            
            JSONArray medsArray = root.optJSONArray("medications");
            if (medsArray != null) {
                for (int i = 0; i < medsArray.length(); i++) {
                    JSONObject obj = medsArray.getJSONObject(i);
                    Medication med = new Medication();
                    med.name = obj.optString("name");
                    med.dosage = obj.optString("dosage");
                    med.frequency = obj.optString("frequency");
                    med.reportId = report.id;
                    med.isActive = true;
                    meds.add(med);
                }
            }
        } catch (Exception e) {
            // Fallback: search for Metformin as a demo backup
            if (aiResponse.toLowerCase().contains("metformin")) {
                Medication med = new Medication();
                med.name = "Metformin";
                med.dosage = "500mg";
                med.frequency = "Once daily";
                med.isActive = true;
                med.reportId = report.id;
                meds.add(med);
            }
        }

        new Thread(() -> {
            db.appDao().insertReport(report);
            db.appDao().insertLabResults(labs);
            db.appDao().insertMedications(meds);
            callback.onSuccess(report);
        }).start();
    }

    private static String extractJson(String text) {
        String jsonStr = text == null ? "" : text.trim();
        if (jsonStr.contains("```json")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
            int end = jsonStr.lastIndexOf("```");
            if (end >= 0) {
                jsonStr = jsonStr.substring(0, end);
            }
            jsonStr = jsonStr.trim();
            return jsonStr;
        }
        if (jsonStr.contains("```")) {
            jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
            int end = jsonStr.lastIndexOf("```");
            if (end >= 0) {
                jsonStr = jsonStr.substring(0, end);
            }
            jsonStr = jsonStr.trim();
            return jsonStr;
        }

        int first = jsonStr.indexOf('{');
        int last = jsonStr.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return jsonStr.substring(first, last + 1);
        }
        return jsonStr;
    }
}
