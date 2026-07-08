package com.lecturelens.data.remote;

import org.junit.Test;

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
}
