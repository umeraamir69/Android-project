package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class SttWordInfo {

    @SerializedName("word")
    public String word;

    /** Speech-to-Text v1 field. */
    @SerializedName("startTime")
    public String startTime;

    /** Speech-to-Text v1 field. */
    @SerializedName("endTime")
    public String endTime;

    /** Speech-to-Text v2 field (kept for mapper compatibility). */
    @SerializedName("startOffset")
    public String startOffset;

    /** Speech-to-Text v2 field (kept for mapper compatibility). */
    @SerializedName("endOffset")
    public String endOffset;

    /** Present when speaker diarization is enabled. */
    @SerializedName("speakerTag")
    public Integer speakerTag;
}
