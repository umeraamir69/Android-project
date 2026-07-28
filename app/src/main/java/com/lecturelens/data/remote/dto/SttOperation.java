package com.lecturelens.data.remote.dto;

import com.google.gson.annotations.SerializedName;

/** Speech-to-Text v1 long-running operation (GCS path). */
public class SttOperation {

    @SerializedName("name")
    public String name;

    @SerializedName("done")
    public boolean done;

    @SerializedName("response")
    public SttRecognizeResponse response;

    @SerializedName("error")
    public SttOperationError error;

    public static final class SttOperationError {
        @SerializedName("code")
        public int code;

        @SerializedName("message")
        public String message;
    }
}
