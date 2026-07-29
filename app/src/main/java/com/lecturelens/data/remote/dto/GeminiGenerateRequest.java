package com.lecturelens.data.remote.dto;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class GeminiGenerateRequest {

    @SerializedName("contents")
    public List<GeminiContent> contents;

    @SerializedName("generationConfig")
    public GeminiGenerationConfig generationConfig;

    public GeminiGenerateRequest(String prompt) {
        this(prompt, GeminiGenerationConfig.notesJson());
    }

    public GeminiGenerateRequest(String prompt, @NonNull GeminiGenerationConfig config) {
        this.contents = Collections.singletonList(
                new GeminiContent(Collections.singletonList(new GeminiPart(prompt))));
        this.generationConfig = config;
    }

    public GeminiGenerateRequest(@NonNull List<GeminiPart> parts,
                                 @NonNull GeminiGenerationConfig config) {
        this.contents = Collections.singletonList(new GeminiContent(parts));
        this.generationConfig = config;
    }

    @NonNull
    public static GeminiGenerateRequest plainText(@NonNull String prompt) {
        return new GeminiGenerateRequest(prompt, GeminiGenerationConfig.plainText());
    }
}
