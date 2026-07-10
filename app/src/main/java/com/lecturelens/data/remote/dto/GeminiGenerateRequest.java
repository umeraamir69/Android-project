package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class GeminiGenerateRequest {

    @SerializedName("contents")
    public List<GeminiContent> contents;

    @SerializedName("generationConfig")
    public GeminiGenerationConfig generationConfig;

    public GeminiGenerateRequest(String prompt) {
        this.contents = Collections.singletonList(
                new GeminiContent(Collections.singletonList(new GeminiPart(prompt))));
        this.generationConfig = GeminiGenerationConfig.notesJson();
    }
}
