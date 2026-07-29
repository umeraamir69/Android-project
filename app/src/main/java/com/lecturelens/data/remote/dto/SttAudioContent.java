package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Inline base64 and/or GCS URI audio for Speech-to-Text v1. */
public class SttAudioContent {

    @SerializedName("content")
    public String content;

    @SerializedName("uri")
    public String uri;

    public SttAudioContent(String content) {
        this.content = content;
    }

    public static SttAudioContent fromUri(String gsUri) {
        SttAudioContent audio = new SttAudioContent(null);
        audio.content = null;
        audio.uri = gsUri;
        return audio;
    }
}
