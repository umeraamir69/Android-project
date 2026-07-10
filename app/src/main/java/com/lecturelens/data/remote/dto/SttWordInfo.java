package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SttWordInfo {

    @SerializedName("word")
    public String word;

    @SerializedName("startOffset")
    public String startOffset;

    @SerializedName("endOffset")
    public String endOffset;
}
