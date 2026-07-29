package com.lecturelens.data.remote;

import com.lecturelens.domain.model.TranscriptSegment;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SttResponseMapperTest {

    @Test
    public void parseDurationMs_seconds() {
        assertEquals(1500L, SttResponseMapper.parseDurationMs("1.5s"));
    }

    @Test
    public void parseDurationMs_millis() {
        assertEquals(250L, SttResponseMapper.parseDurationMs("250ms"));
    }

    @Test
    public void groupBySecond_splitsDifferentSpeakers() {
        List<SttResponseMapper.TimedWord> words = Arrays.asList(
                new SttResponseMapper.TimedWord(0, 200, "Hello", 1),
                new SttResponseMapper.TimedWord(200, 400, "there", 1),
                new SttResponseMapper.TimedWord(400, 600, "Hi", 2)
        );
        List<TranscriptSegment> segments = SttResponseMapper.groupBySecond(9L, words);
        assertEquals(2, segments.size());
        assertEquals(1, segments.get(0).getSpeakerTag());
        assertEquals("Hello there", segments.get(0).getText());
        assertEquals(2, segments.get(1).getSpeakerTag());
        assertEquals("Hi", segments.get(1).getText());
    }
}
