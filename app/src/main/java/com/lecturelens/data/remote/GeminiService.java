package com.lecturelens.data.remote;

import com.lecturelens.data.remote.dto.GeminiGenerateRequest;
import com.lecturelens.data.remote.dto.GeminiGenerateResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Google AI Gemini REST API (AI Studio key).
 */
public interface GeminiService {

    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiGenerateResponse> generateContent(
            @Path("model") String model,
            @Header("x-goog-api-key") String apiKey,
            @Body GeminiGenerateRequest request);
}
