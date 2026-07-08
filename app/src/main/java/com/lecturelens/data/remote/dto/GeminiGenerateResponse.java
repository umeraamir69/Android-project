package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeminiGenerateResponse {

    @SerializedName("candidates")
    public List<GeminiCandidate> candidates;
}
