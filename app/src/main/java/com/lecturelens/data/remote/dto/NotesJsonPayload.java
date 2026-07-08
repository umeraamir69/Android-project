package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Parsed JSON body returned by Gemini when summarizing a lecture. */
public class NotesJsonPayload {

    @SerializedName("summary")
    public String summary;

    @SerializedName("keyTerms")
    public List<String> keyTerms;

    @SerializedName("actionItems")
    public List<String> actionItems;
}
