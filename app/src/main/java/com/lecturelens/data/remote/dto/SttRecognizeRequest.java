package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Speech-to-Text v1 request (sync recognize or longRunningRecognize).
 */
public class SttRecognizeRequest {

    @SerializedName("config")
    public SttRecognitionConfig config;

    @SerializedName("audio")
    public SttAudioContent audio;

    public SttRecognizeRequest(SttRecognitionConfig config, String base64Content) {
        this.config = config;
        this.audio = new SttAudioContent(base64Content);
    }

    public SttRecognizeRequest(SttRecognitionConfig config, SttAudioContent audio) {
        this.config = config;
        this.audio = audio;
    }
}
