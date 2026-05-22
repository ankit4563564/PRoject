package com.medisync.diabo.service;

import android.content.Context;
import com.medisync.diabo.BuildConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ChatService that routes AI requests through the local Ollama backend.
 * Falls back through multiple endpoints via OllamaRouter.
 */
public class ChatService {

    private final String model;
    private final OkHttpClient httpClient;

    public ChatService() {
        this.model = BuildConfig.OLLAMA_MODEL;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /** Legacy constructor for backward compat — ignores the apiKey */
    public ChatService(String apiKey) {
        this();
    }

    public void chatWithAI(String message, OllamaRouter.TextCallback callback) {
        String prompt = "You are a helpful, empathetic, and knowledgeable clinical companion chatbot for a diabetic patient. " +
                "Answer the user's query with practical, easy-to-understand medical advice, diet recommendations, or medication explanations. " +
                "Keep your response concise, clinical, and conversational.\n" +
                "User Query: " + message;

        OllamaApi.Request request = new OllamaApi.Request(model, prompt, 350);
        tryEndpoints(buildEndpoints(), 0, request, callback);
    }

    public void analyzeReport(String extractedText, OllamaRouter.TextCallback callback) {
        String prompt = "You are a clinical assistant. Analyze the following medical report text and extract lab results and medications. " +
                "Return ONLY a JSON object with two arrays: 'lab_results' (each with 'name', 'value', 'unit', 'range', 'status', 'testDate') " +
                "and 'medications' (each with 'name', 'dosage', 'frequency', 'reason'). " +
                "Text: " + extractedText;

        OllamaApi.Request request = new OllamaApi.Request(model, prompt, 700);
        tryEndpoints(buildEndpoints(), 0, request, callback);
    }

    private java.util.List<String> buildEndpoints() {
        java.util.LinkedHashSet<String> urls = new java.util.LinkedHashSet<>();
        addEndpoint(urls, BuildConfig.OLLAMA_BASE_URL);
        addEndpoint(urls, "http://10.0.2.2:11434/");
        addEndpoint(urls, "http://127.0.0.1:11434/");
        addEndpoint(urls, "http://localhost:11434/");
        return new java.util.ArrayList<>(urls);
    }

    private void addEndpoint(java.util.LinkedHashSet<String> urls, String url) {
        if (url == null) {
            return;
        }
        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        urls.add(trimmed.endsWith("/") ? trimmed : trimmed + "/");
    }

    private void tryEndpoints(java.util.List<String> urls, int idx,
                              OllamaApi.Request request, OllamaRouter.TextCallback cb) {
        if (idx >= urls.size()) {
            cb.onFailure("All Ollama endpoints unreachable. Make sure 'ollama serve' is running and adb reverse is set up.");
            return;
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(urls.get(idx))
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        OllamaApi api = retrofit.create(OllamaApi.class);
        api.generate(request).enqueue(new Callback<OllamaApi.Response>() {
            @Override
            public void onResponse(Call<OllamaApi.Response> call, Response<OllamaApi.Response> response) {
                if (response.isSuccessful() && response.body() != null
                        && !response.body().getText().trim().isEmpty()) {
                    cb.onSuccess(response.body().getText());
                } else {
                    tryEndpoints(urls, idx + 1, request, cb);
                }
            }

            @Override
            public void onFailure(Call<OllamaApi.Response> call, Throwable t) {
                android.util.Log.w("ChatService", "Endpoint " + urls.get(idx) + " failed: " + t.getMessage());
                tryEndpoints(urls, idx + 1, request, cb);
            }
        });
    }
}
