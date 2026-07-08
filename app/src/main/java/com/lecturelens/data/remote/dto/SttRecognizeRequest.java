package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Speech-to-Text v2 synchronous recognize request (inline audio, auto-decode).
 */
public class SttRecognizeRequest {

    @SerializedName("config")
    public SttRecognitionConfig config;

    @SerializedName("content")
    public String content;

    public SttRecognizeRequest(SttRecognitionConfig config, String content) {
        this.config = config;
        this.content = content;
    }
}
