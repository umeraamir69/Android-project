package com.lecturelens.data.remote.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.util.Locale;

/** Speech-to-Text v1 recognition config. */
public class SttRecognitionConfig {

    @SerializedName("encoding")
    public String encoding;

    @SerializedName("sampleRateHertz")
    @Nullable
    public Integer sampleRateHertz;

    @SerializedName("languageCode")
    public String languageCode;

    @SerializedName("enableWordTimeOffsets")
    public boolean enableWordTimeOffsets = true;

    @SerializedName("enableAutomaticPunctuation")
    public boolean enableAutomaticPunctuation = true;

    @SerializedName("model")
    public String model = "latest_long";

    /** Legacy v1 diarization flag (still accepted). */
    @SerializedName("enableSpeakerDiarization")
    public Boolean enableSpeakerDiarization;

    @SerializedName("diarizationConfig")
    @Nullable
    public DiarizationConfig diarizationConfig;

    public SttRecognitionConfig(@NonNull String encoding,
                                @Nullable Integer sampleRateHz,
                                @NonNull String languageCode) {
        this.encoding = encoding;
        this.sampleRateHertz = sampleRateHz;
        this.languageCode = languageCode;
        // Diarization is opt-in — enabling it by default breaks many API-key
        // projects / models with INVALID_ARGUMENT on speech:recognize.
    }

    public void enableDiarization(int minSpeakers, int maxSpeakers) {
        enableSpeakerDiarization = true;
        DiarizationConfig config = new DiarizationConfig();
        config.enableSpeakerDiarization = true;
        config.minSpeakerCount = Math.max(1, minSpeakers);
        config.maxSpeakerCount = Math.max(config.minSpeakerCount, maxSpeakers);
        diarizationConfig = config;
    }

    @Nullable
    public static SttRecognitionConfig forAudioFile(@NonNull File audio,
                                                    @NonNull String languageCode) {
        String name = audio.getName().toLowerCase(Locale.US);
        if (name.endsWith(".amr")) {
            return new SttRecognitionConfig("AMR_WB", 16_000, languageCode);
        }
        if (name.endsWith(".flac")) {
            return new SttRecognitionConfig("FLAC", null, languageCode);
        }
        if (name.endsWith(".wav")) {
            return new SttRecognitionConfig("LINEAR16", 16_000, languageCode);
        }
        if (name.endsWith(".mp3")) {
            return new SttRecognitionConfig("MP3", null, languageCode);
        }
        if (name.endsWith(".ogg") || name.endsWith(".opus")) {
            return new SttRecognitionConfig("OGG_OPUS", null, languageCode);
        }
        return null;
    }

    public static final class DiarizationConfig {
        @SerializedName("enableSpeakerDiarization")
        public boolean enableSpeakerDiarization = true;

        @SerializedName("minSpeakerCount")
        public int minSpeakerCount = 1;

        @SerializedName("maxSpeakerCount")
        public int maxSpeakerCount = 6;
    }
}
