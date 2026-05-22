package com.medisync.diabo.service;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface OllamaApi {
    class Request {
        public String model;
        public String prompt;
        public boolean stream;
        public Map<String, Object> options;

        public Request(String model, String prompt) {
            this.model = model;
            this.prompt = prompt;
            this.stream = false;
            this.options = new HashMap<>();
            this.options.put("temperature", 0.2);
            this.options.put("num_predict", 512);
        }

        public Request(String model, String prompt, int numPredict) {
            this(model, prompt);
            this.options.put("num_predict", numPredict);
        }
    }

    class Response {
        @SerializedName("response")
        public String response;

        @SerializedName("thinking")
        public String thinking;

        public String getText() {
            if (response != null && !response.trim().isEmpty()) {
                return response;
            }
            if (thinking != null && !thinking.trim().isEmpty()) {
                return thinking;
            }
            return "";
        }
    }

    @POST("api/generate")
    Call<Response> generate(@Body Request request);

    @retrofit2.http.GET(".")
    Call<okhttp3.ResponseBody> checkStatus();
}
