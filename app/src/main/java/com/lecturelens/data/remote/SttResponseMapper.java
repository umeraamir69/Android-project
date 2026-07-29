package com.lecturelens.data.remote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.data.remote.dto.SttAlternative;
import com.lecturelens.data.remote.dto.SttRecognizeResponse;
import com.lecturelens.data.remote.dto.SttSpeechResult;
import com.lecturelens.data.remote.dto.SttWordInfo;
import com.lecturelens.domain.model.TranscriptSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps Speech-to-Text recognize responses to domain transcript segments.
 *
 * <p>Words are grouped by whole second <b>and</b> speaker tag so the Transcript
 * tab stays readable and shows speaker labels when diarization is on.
 */
public final class SttResponseMapper {

    private SttResponseMapper() {
    }

    @NonNull
    public static String extractFullText(@NonNull SttRecognizeResponse response) {
        if (response.results == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SttSpeechResult result : response.results) {
            if (result.alternatives == null || result.alternatives.isEmpty()) {
                continue;
            }
            String line = result.alternatives.get(0).transcript;
            if (line == null || line.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(line.trim());
        }
        return sb.toString().trim();
    }

    @NonNull
    public static List<TranscriptSegment> toSegments(long lectureId,
                                                     @NonNull SttRecognizeResponse response) {
        return toSegments(lectureId, response, 0L);
    }

    /**
     * @param timeOffsetMs added to every word timestamp (used when stitching
     *                     chunked sync-recognize results for long audio).
     */
    @NonNull
    public static List<TranscriptSegment> toSegments(long lectureId,
                                                     @NonNull SttRecognizeResponse response,
                                                     long timeOffsetMs) {
        if (response.results == null) {
            return Collections.emptyList();
        }

        List<TimedWord> words = new ArrayList<>();
        List<TranscriptSegment> phraseFallback = new ArrayList<>();
        long nextId = 1L;

        for (SttSpeechResult result : response.results) {
            if (result.alternatives == null || result.alternatives.isEmpty()) {
                continue;
            }
            SttAlternative alt = result.alternatives.get(0);
            if (alt.words != null && !alt.words.isEmpty()) {
                for (SttWordInfo word : alt.words) {
                    if (word.word == null || word.word.isEmpty()) {
                        continue;
                    }
                    long start = parseDurationMs(firstNonEmpty(word.startTime, word.startOffset))
                            + timeOffsetMs;
                    long end = parseDurationMs(firstNonEmpty(word.endTime, word.endOffset))
                            + timeOffsetMs;
                    if (end < start) {
                        end = start;
                    }
                    int speaker = word.speakerTag != null ? word.speakerTag : 0;
                    words.add(new TimedWord(start, end, word.word.trim(), speaker));
                }
            } else if (alt.transcript != null && !alt.transcript.isEmpty()) {
                phraseFallback.add(new TranscriptSegment(
                        nextId++, lectureId, timeOffsetMs, timeOffsetMs, alt.transcript.trim(), 0));
            }
        }

        if (!words.isEmpty()) {
            return groupBySecondAndSpeaker(lectureId, words);
        }
        return phraseFallback;
    }

    /**
     * Buckets by {@code (second, speakerTag)} so mixed-speaker seconds stay split.
     */
    @NonNull
    static List<TranscriptSegment> groupBySecond(long lectureId,
                                                 @NonNull List<TimedWord> words) {
        return groupBySecondAndSpeaker(lectureId, words);
    }

    @NonNull
    static List<TranscriptSegment> groupBySecondAndSpeaker(long lectureId,
                                                           @NonNull List<TimedWord> words) {
        Map<String, SecondBucket> buckets = new LinkedHashMap<>();
        for (TimedWord word : words) {
            long second = Math.max(0L, word.startMs) / 1000L;
            String key = second + ":" + word.speakerTag;
            SecondBucket bucket = buckets.get(key);
            if (bucket == null) {
                bucket = new SecondBucket(second * 1000L, word.speakerTag);
                buckets.put(key, bucket);
            }
            bucket.add(word);
        }

        List<TranscriptSegment> segments = new ArrayList<>(buckets.size());
        long nextId = 1L;
        for (SecondBucket bucket : buckets.values()) {
            segments.add(new TranscriptSegment(
                    nextId++,
                    lectureId,
                    bucket.startMs,
                    bucket.endMs,
                    bucket.text(),
                    bucket.speakerTag));
        }
        return segments;
    }

    @Nullable
    private static String firstNonEmpty(@Nullable String a, @Nullable String b) {
        if (a != null && !a.isEmpty()) {
            return a;
        }
        if (b != null && !b.isEmpty()) {
            return b;
        }
        return null;
    }

    static long parseDurationMs(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }
        String value = raw.trim().toLowerCase(Locale.US);
        try {
            if (value.endsWith("ms")) {
                return Math.round(Double.parseDouble(value.substring(0, value.length() - 2)));
            }
            if (value.endsWith("s")) {
                return Math.round(Double.parseDouble(value.substring(0, value.length() - 1)) * 1000.0);
            }
            return Math.round(Double.parseDouble(value) * 1000.0);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    static final class TimedWord {
        final long startMs;
        final long endMs;
        @NonNull final String text;
        final int speakerTag;

        TimedWord(long startMs, long endMs, @NonNull String text) {
            this(startMs, endMs, text, 0);
        }

        TimedWord(long startMs, long endMs, @NonNull String text, int speakerTag) {
            this.startMs = startMs;
            this.endMs = endMs;
            this.text = text;
            this.speakerTag = Math.max(0, speakerTag);
        }
    }

    private static final class SecondBucket {
        final long startMs;
        final int speakerTag;
        long endMs;
        private final StringBuilder text = new StringBuilder();

        SecondBucket(long startMs, int speakerTag) {
            this.startMs = startMs;
            this.endMs = startMs;
            this.speakerTag = speakerTag;
        }

        void add(@NonNull TimedWord word) {
            if (text.length() > 0) {
                text.append(' ');
            }
            text.append(word.text);
            if (word.endMs > endMs) {
                endMs = word.endMs;
            }
        }

        @NonNull
        String text() {
            return text.toString();
        }
    }
}
