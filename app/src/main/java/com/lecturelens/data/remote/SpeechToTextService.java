package com.lecturelens.data.remote;

import com.lecturelens.data.remote.dto.SttOperation;
import com.lecturelens.data.remote.dto.SttRecognizeRequest;
import com.lecturelens.data.remote.dto.SttRecognizeResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Google Cloud Speech-to-Text v1 — sync recognize + optional long-running (GCS).
 */
public interface SpeechToTextService {

    @POST("v1/speech:recognize")
    Call<SttRecognizeResponse> recognize(
            @Query("key") String apiKey,
            @Body SttRecognizeRequest request);

    @POST("v1/speech:longrunningrecognize")
    Call<SttOperation> longRunningRecognize(
            @Query("key") String apiKey,
            @Body SttRecognizeRequest request);

    @GET("v1/operations/{name}")
    Call<SttOperation> getOperation(
            @Path(value = "name", encoded = true) String name,
            @Query("key") String apiKey);
}
