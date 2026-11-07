package com.wishat12.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneUtilsTest {
    @Test fun removesFormattingAndInternationalDialPrefix() {
        assertEquals("919876543210", PhoneUtils.normalize("+91 98765-43210"))
        assertEquals("919876543210", PhoneUtils.normalize("0091 98765 43210"))
    }
    @Test fun validatesCountryCodeNumberLength() {
        assertTrue(PhoneUtils.isValid("+919876543210"))
        assertFalse(PhoneUtils.isValid("123"))
        assertFalse(PhoneUtils.isValid("09876543210"))
    }
}
