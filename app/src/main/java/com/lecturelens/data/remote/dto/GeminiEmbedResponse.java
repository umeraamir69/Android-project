package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Gemini embedContent response. */
public class GeminiEmbedResponse {

    @SerializedName("embedding")
    public Embedding embedding;

    public static final class Embedding {
        @SerializedName("values")
        public List<Float> values;
    }
}
