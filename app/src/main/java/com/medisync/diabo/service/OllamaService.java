package com.medisync.diabo.service;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class OllamaService {
    private final OllamaApi api;
    private final String model;

    public OllamaService(String baseUrl, String model) {
        this.model = model;
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        this.api = retrofit.create(OllamaApi.class);
    }

    public void analyzeReport(String extractedText, Callback<OllamaApi.Response> callback) {
        String prompt = "You are a clinical assistant. Analyze the following medical report text and extract lab results and medications. " +
                "Return ONLY JSON with two arrays: lab_results (name,value,unit,range,status,testDate) and medications (name,dosage,frequency,reason). " +
                "Text: " + extractedText;
        api.generate(new OllamaApi.Request(model, prompt, 700)).enqueue(callback);
    }

    public void ping(Callback<okhttp3.ResponseBody> callback) {
        api.checkStatus().enqueue(callback);
    }

    public String getModel() {
        return model;
    }
}
