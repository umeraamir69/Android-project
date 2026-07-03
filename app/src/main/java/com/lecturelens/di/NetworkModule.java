package com.lecturelens.di;

import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * SKELETON — Track 4 (Muhammad) fills this in.
 *
 * Will provide: OkHttpClient (API-key interceptor + HttpLoggingInterceptor
 * in debug), Retrofit instances for SpeechToTextService and GeminiService.
 * Keep providers @Singleton; base URLs as constants here, keys from
 * SecureKeyStore (Track 1, week 2).
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {
    // TODO(Track 4): provide OkHttpClient + Retrofit services.
}
