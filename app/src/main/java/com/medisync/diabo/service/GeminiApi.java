package com.medisync.diabo.service;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;
import java.util.List;

public interface GeminiApi {

    class Request {
        public List<Content> contents;
        public Request(String text) {
            this.contents = List.of(new Content(text));
        }
    }

    class Content {
        public List<Part> parts;
        public Content(String text) {
            this.parts = List.of(new Part(text));
        }
    }

    class Part {
        public String text;
        public Part(String text) {
            this.text = text;
        }
    }

    class Response {
        public List<Candidate> candidates;
        public String getText() {
            if (candidates != null && !candidates.isEmpty()) {
                return candidates.get(0).content.parts.get(0).text;
            }
            return "";
        }
    }

    class Candidate {
        public Content content;
    }

    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<Response> generateContent(@Query("key") String apiKey, @Body Request request);
}
