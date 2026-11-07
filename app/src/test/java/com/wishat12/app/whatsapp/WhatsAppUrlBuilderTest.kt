package com.wishat12.app.whatsapp

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppUrlBuilderTest {
    @Test fun encodesMessageAndNormalizesPhone() {
        val url = WhatsAppLauncher.buildUrl("+91 98765 43210", "Happy birthday, Yash! ❤️")
        assertNotNull(url)
        assertTrue(url!!.startsWith("https://wa.me/919876543210?text="))
        assertTrue(url.contains("%20") || url.contains("+"))
    }
    @Test fun rejectsInvalidPhone() {
        assertNull(WhatsAppLauncher.buildUrl("123", "hello"))
    }
}
