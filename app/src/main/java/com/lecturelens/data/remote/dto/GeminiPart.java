package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class GeminiPart {

    @SerializedName("text")
    public String text;

    public GeminiPart(String text) {
        this.text = text;
    }
}
