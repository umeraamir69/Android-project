package com.lecturelens.data.remote;

import com.lecturelens.data.remote.dto.GeminiEmbedRequest;
import com.lecturelens.data.remote.dto.GeminiEmbedResponse;
import com.lecturelens.data.remote.dto.GeminiGenerateRequest;
import com.lecturelens.data.remote.dto.GeminiGenerateResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Google AI Gemini REST API.
 * Uses {@code X-goog-api-key} (same as AI Studio curl) so newer {@code AQ.} keys work.
 */
public interface GeminiService {

    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiGenerateResponse> generateContent(
            @Path(value = "model", encoded = true) String model,
            @Header("X-goog-api-key") String apiKey,
            @Body GeminiGenerateRequest request);

    @POST("v1beta/models/{model}:embedContent")
    Call<GeminiEmbedResponse> embedContent(
            @Path(value = "model", encoded = true) String model,
            @Header("X-goog-api-key") String apiKey,
            @Body GeminiEmbedRequest request);
}
