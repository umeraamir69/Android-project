package com.lecturelens.data.remote;

import androidx.annotation.NonNull;

import com.lecturelens.data.remote.dto.SttAlternative;
import com.lecturelens.data.remote.dto.SttRecognizeResponse;
import com.lecturelens.data.remote.dto.SttSpeechResult;
import com.lecturelens.data.remote.dto.SttWordInfo;
import com.lecturelens.domain.model.TranscriptSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Maps Speech-to-Text v2 responses to domain transcript segments.
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
        if (response.results == null) {
            return Collections.emptyList();
        }
        List<TranscriptSegment> segments = new ArrayList<>();
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
                    long start = parseDurationMs(word.startOffset);
                    long end = parseDurationMs(word.endOffset);
                    segments.add(new TranscriptSegment(nextId++, lectureId, start, end, word.word));
                }
            } else if (alt.transcript != null && !alt.transcript.isEmpty()) {
                segments.add(new TranscriptSegment(nextId++, lectureId, 0L, 0L, alt.transcript));
            }
        }
        return segments;
    }

    /** Parses {@code 1.234s} or {@code 1234ms} style offsets from the v2 API. */
    static long parseDurationMs(@NonNull String raw) {
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
}
