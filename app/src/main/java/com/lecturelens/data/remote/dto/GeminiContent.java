package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeminiContent {

    @SerializedName("parts")
    public List<GeminiPart> parts;

    public GeminiContent(List<GeminiPart> parts) {
        this.parts = parts;
    }
}
