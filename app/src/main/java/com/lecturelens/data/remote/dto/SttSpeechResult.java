package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SttSpeechResult {

    @SerializedName("alternatives")
    public List<SttAlternative> alternatives;

    @SerializedName("languageCode")
    public String languageCode;
}
