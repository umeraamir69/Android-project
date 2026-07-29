package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

/** Gemini embedContent request. */
public class GeminiEmbedRequest {

    @SerializedName("content")
    public final GeminiContent content;

    public GeminiEmbedRequest(String text) {
        this.content = new GeminiContent(Collections.singletonList(new GeminiPart(text)));
    }

    public static final class GeminiContent {
        @SerializedName("parts")
        public final List<GeminiPart> parts;

        public GeminiContent(List<GeminiPart> parts) {
            this.parts = parts;
        }
    }
}
