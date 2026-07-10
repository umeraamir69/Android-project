package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SttAlternative {

    @SerializedName("transcript")
    public String transcript;

    @SerializedName("words")
    public List<SttWordInfo> words;
}
