package com.lecturelens.di;

import com.lecturelens.BuildConfig;
import com.lecturelens.data.remote.GeminiService;
import com.lecturelens.data.remote.SpeechToTextService;

import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Track 4 — Retrofit + OkHttp for Google Cloud Speech-to-Text and Gemini.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String SPEECH_BASE_URL = "https://speech.googleapis.com/";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/";

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS);
        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
            builder.addInterceptor(logging);
        }
        return builder.build();
    }

    @Provides
    @Singleton
    static SpeechToTextService provideSpeechToTextService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(SPEECH_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SpeechToTextService.class);
    }

    @Provides
    @Singleton
    static GeminiService provideGeminiService(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(GEMINI_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeminiService.class);
    }
}
