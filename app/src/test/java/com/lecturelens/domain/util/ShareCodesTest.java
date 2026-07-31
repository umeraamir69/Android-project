package com.lecturelens.domain.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ShareCodesTest {

    @Test
    public void normalize_trimsAndUppercases() {
        assertEquals("ABC234", ShareCodes.normalize(" abc234 "));
    }

    @Test
    public void isValidFormat_requiresExactLengthAndAlphabet() {
        assertTrue(ShareCodes.isValidFormat("ABCD23"));
        assertFalse(ShareCodes.isValidFormat("ABC"));
        assertFalse(ShareCodes.isValidFormat("ABCDEFG"));
        assertFalse(ShareCodes.isValidFormat("ABCD01")); // 0/1 not in alphabet
        assertFalse(ShareCodes.isValidFormat(""));
        assertFalse(ShareCodes.isValidFormat(null));
    }
}
