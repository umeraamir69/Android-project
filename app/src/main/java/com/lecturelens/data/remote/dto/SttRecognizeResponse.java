package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SttRecognizeResponse {

    @SerializedName("results")
    public List<SttSpeechResult> results;

    @SerializedName("metadata")
    public SttResponseMetadata metadata;
}
