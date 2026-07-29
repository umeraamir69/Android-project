package com.lecturelens.data.remote.dto;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class GeminiPart {

    @SerializedName("text")
    public String text;

    @SerializedName("inline_data")
    public InlineData inlineData;

    public GeminiPart(String text) {
        this.text = text;
    }

    @NonNull
    public static GeminiPart image(@NonNull String mimeType, @NonNull String base64) {
        GeminiPart part = new GeminiPart(null);
        part.inlineData = new InlineData(mimeType, base64);
        return part;
    }

    public static final class InlineData {
        @SerializedName("mime_type")
        public final String mimeType;

        @SerializedName("data")
        public final String data;

        public InlineData(String mimeType, String data) {
            this.mimeType = mimeType;
            this.data = data;
        }
    }
}
