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

    /** Short lecture title suggested from the transcript. */
    @SerializedName("title")
    public String title;

    /** Suggested course/category name (may match an existing category). */
    @SerializedName("category")
    public String category;
}
