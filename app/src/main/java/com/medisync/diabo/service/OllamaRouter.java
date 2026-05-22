package com.medisync.diabo.service;

import android.content.Context;
import android.util.Log;
import com.medisync.diabo.BuildConfig;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OllamaRouter {
    private static final String TAG = "OllamaRouter";
    public interface TextCallback {
        void onSuccess(String text);
        void onFailure(String error);
    }

    private static List<String> endpoints() {
        Set<String> urls = new LinkedHashSet<>();
        addEndpoint(urls, BuildConfig.OLLAMA_BASE_URL);
        addEndpoint(urls, "http://10.0.2.2:11434/");
        addEndpoint(urls, "http://127.0.0.1:11434/");
        addEndpoint(urls, "http://localhost:11434/");
        return new java.util.ArrayList<>(urls);
    }

    private static void addEndpoint(Set<String> urls, String url) {
        if (url == null) {
            return;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        urls.add(trimmed.endsWith("/") ? trimmed : trimmed + "/");
    }

    public static void analyzeReport(Context context, String model, String extractedText, TextCallback cb) {
        List<String> urls = endpoints();
        tryAnalyze(urls, 0, model, extractedText, cb);
    }

    public static void ping(Context context, String model, TextCallback cb) {
        List<String> urls = endpoints();
        tryPing(urls, 0, model, cb);
    }

    private static void tryAnalyze(List<String> urls, int i, String model, String text, TextCallback cb) {
        if (i >= urls.size()) {
            cb.onFailure("All Ollama endpoints failed");
            return;
        }
        OllamaService svc = new OllamaService(urls.get(i), model);
        svc.analyzeReport(text, new Callback<OllamaApi.Response>() {
            @Override
            public void onResponse(Call<OllamaApi.Response> call, Response<OllamaApi.Response> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().getText().trim().isEmpty()) {
                    cb.onSuccess(response.body().getText());
                } else {
                    Log.e(TAG, "Analyze non-success: " + describeResponse(urls.get(i), response));
                    tryAnalyze(urls, i + 1, model, text, cb);
                }
            }

            @Override
            public void onFailure(Call<OllamaApi.Response> call, Throwable t) {
                Log.e(TAG, "Analyze failed at " + urls.get(i) + ": " + t.getMessage(), t);
                tryAnalyze(urls, i + 1, model, text, cb);
            }
        });
    }

    private static void tryPing(List<String> urls, int i, String model, TextCallback cb) {
        if (i >= urls.size()) {
            cb.onFailure("All Ollama endpoints failed. Check ollama serve + adb reverse.");
            return;
        }
        OllamaService svc = new OllamaService(urls.get(i), model);
        svc.ping(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {
                    cb.onSuccess("Connected via " + urls.get(i));
                } else {
                    String reason = describeResponse(urls.get(i), response);
                    Log.e(TAG, "Ping non-success: " + reason);
                    if (i == urls.size() - 1) {
                        cb.onFailure(reason);
                        return;
                    }
                    tryPing(urls, i + 1, model, cb);
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                String reason = t.getClass().getSimpleName() + " at " + urls.get(i) + " : " + t.getMessage();
                Log.e(TAG, "Ping failed: " + reason);
                if (i == urls.size() - 1) {
                    cb.onFailure(reason);
                    return;
                }
                tryPing(urls, i + 1, model, cb);
            }
        });
    }

    private static String describeResponse(String url, Response<?> response) {
        return "HTTP " + response.code() + " at " + url;
    }
}
