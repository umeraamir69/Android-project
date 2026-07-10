package com.lecturelens.data.remote;

import com.lecturelens.data.remote.dto.SttRecognizeRequest;
import com.lecturelens.data.remote.dto.SttRecognizeResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Google Cloud Speech-to-Text v2 — synchronous recognize with inline audio.
 */
public interface SpeechToTextService {

    @POST("v2/projects/{projectId}/locations/global/recognizers/_:recognize")
    Call<SttRecognizeResponse> recognize(
            @Path("projectId") String projectId,
            @Query("key") String apiKey,
            @Body SttRecognizeRequest request);
}
