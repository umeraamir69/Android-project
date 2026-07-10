package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class SttRecognitionConfig {

    @SerializedName("autoDecodingConfig")
    public Object autoDecodingConfig = new Object();

    @SerializedName("languageCodes")
    public List<String> languageCodes;

    @SerializedName("model")
    public String model = "long";

    @SerializedName("features")
    public SttFeatures features;

    public SttRecognitionConfig(List<String> languageCodes) {
        this.languageCodes = languageCodes;
        this.features = new SttFeatures();
    }

    public static SttRecognitionConfig forLanguage(String languageCode) {
        return new SttRecognitionConfig(Collections.singletonList(languageCode));
    }
}
