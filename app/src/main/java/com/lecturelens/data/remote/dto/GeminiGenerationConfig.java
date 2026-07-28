package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class GeminiGenerationConfig {

    @SerializedName("temperature")
    public float temperature;

    @SerializedName("responseMimeType")
    public String responseMimeType;

    public static GeminiGenerationConfig notesJson() {
        GeminiGenerationConfig config = new GeminiGenerationConfig();
        config.temperature = 0.2f;
        config.responseMimeType = "application/json";
        return config;
    }

    /** Plain-text answers for notes Q&A (grounded chat). */
    public static GeminiGenerationConfig plainText() {
        GeminiGenerationConfig config = new GeminiGenerationConfig();
        config.temperature = 0.2f;
        config.responseMimeType = "text/plain";
        return config;
    }
}
